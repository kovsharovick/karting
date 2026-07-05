package ru.vsu.cs.yesikov.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.dto.RatingSummaryDto;
import ru.vsu.cs.yesikov.dto.booking.BookingListResponse;
import ru.vsu.cs.yesikov.dto.booking.BookingResponse;
import ru.vsu.cs.yesikov.dto.booking.BookingSummaryResponse;
import ru.vsu.cs.yesikov.dto.booking.CreateBookingRequest;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;
import ru.vsu.cs.yesikov.dto.slot.SlotResponse;
import ru.vsu.cs.yesikov.dto.slot.SlotSummaryResponse;
import ru.vsu.cs.yesikov.exception.BusinessException;
import ru.vsu.cs.yesikov.exception.SqlErrorMapper;
import ru.vsu.cs.yesikov.model.Booking;
import ru.vsu.cs.yesikov.model.Client;
import ru.vsu.cs.yesikov.model.IdempotencyKey;
import ru.vsu.cs.yesikov.model.Slot;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;
import ru.vsu.cs.yesikov.repository.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private static final String CREATE_BOOKING_ENDPOINT = "POST /bookings";

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final ClientRepository clientRepository;
    private final RatingRepository ratingRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final SlotService slotService;

    @Value("${spring.profiles.active:}")
    private String activeProfile;


    public BookingResponse createBooking(UUID clientId, CreateBookingRequest request, UUID idempotencyKey) {
        String fingerprint = buildFingerprint(request);

        // ---- Шаг 1: проверка идемпотентности (LOGIC-003, шаг 1) ----
        // Тот же Idempotency-Key + то же тело запроса -> это повтор той же попытки
        // (например, после сетевого сбоя на клиенте) - возвращаем уже созданную бронь,
        // а не создаём вторую и не отдаём ошибку.
        Optional<IdempotencyKey> existingIdempotent = idempotencyKeyRepository
                .findByIdempotencyKeyAndClientIdAndEndpoint(idempotencyKey, clientId, CREATE_BOOKING_ENDPOINT);

        if (existingIdempotent.isPresent()) {
            IdempotencyKey ik = existingIdempotent.get();

            if (!ik.getRequestFingerprint().equals(fingerprint)) {
                // Тот же ключ, но другое тело запроса - это уже нарушение контракта клиента.
                throw new BusinessException(
                        "Idempotency-Key уже был использован с другим телом запроса",
                        HttpStatus.CONFLICT,
                        "idempotency_key_conflict"
                );
            }

            Booking existingBooking = bookingRepository
                    .findByIdempotencyKeyAndClientId(idempotencyKey, clientId)
                    .orElseThrow(() -> new BusinessException(
                            "Бронь по этому Idempotency-Key не найдена",
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "internal_error"
                    ));

            log.info("Idempotent replay for key {} (client {}): returning existing booking {}",
                    idempotencyKey, clientId, existingBooking.getId());
            return toBookingResponse(existingBooking);
        }

        // ---- Шаг 2: обычное создание брони ----
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client not found", HttpStatus.NOT_FOUND, "not_found"));

        Slot slot = slotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new BusinessException("Slot not found", HttpStatus.NOT_FOUND, "not_found"));

        if (slot.getStatus() == SlotStatus.cancelled) {
            throw new BusinessException("Заезд отменён администрацией", HttpStatus.GONE, "slot_cancelled");
        }

        // Заезд уже начался - запись недоступна (LOGIC-003 шаг 4, 422 slot_started).
        if (!OffsetDateTime.now().isBefore(slot.getStartAt())) {
            throw new BusinessException(
                    "Заезд уже начался, операция недоступна",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "slot_started"
            );
        }

        if (bookingRepository.existsActiveBookingForClientAndSlot(clientId, slot.getId())) {
            throw new BusinessException(
                    "У вас уже есть активная запись на этот заезд",
                    HttpStatus.CONFLICT,
                    "double_booking"
            );
        }

        // Предварительные (advisory) проверки лимитов - дают правильный контрактный код ошибки
        // без обращения к БД. Финальную атомарную проверку в любом случае делает триггер
        // reserve_slot_inventory() при INSERT (SELECT ... FOR UPDATE) - если данные успели
        // устареть между этой проверкой и вставкой, ошибка триггера будет поймана и
        // замаплена ниже через SqlErrorMapper.
        if (request.getSeatsCount() > slot.getFreeKarts()) {
            throw new BusinessException(
                    "Свободных мест больше нет",
                    HttpStatus.CONFLICT,
                    "slot_full",
                    Map.of("free_karts", slot.getFreeKarts())
            );
        }

        if (request.getRentalGearCount() > slot.getFreeRentalGear()) {
            throw new BusinessException(
                    "Недостаточно прокатной экипировки",
                    HttpStatus.CONFLICT,
                    "gear_unavailable",
                    Map.of("free_rental_gear", slot.getFreeRentalGear())
            );
        }

        boolean managesInventoryInApplication = managesInventoryInApplication();
        if (managesInventoryInApplication) {
            slot.setFreeKarts((short) (slot.getFreeKarts() - request.getSeatsCount()));
            slot.setFreeRentalGear((short) (slot.getFreeRentalGear() - request.getRentalGearCount()));
        }

        int priceTotal = slot.getPriceKart() * request.getSeatsCount() +
                slot.getPriceGearRental() * request.getRentalGearCount();

        Booking booking = Booking.builder()
                .slot(slot)
                .client(client)
                .seatsCount(request.getSeatsCount())
                .rentalGearCount(request.getRentalGearCount())
                .status(BookingStatus.active)
                .priceTotal(priceTotal)
                .idempotencyKey(idempotencyKey)
                .build();

        Booking saved;
        try {
            saved = bookingRepository.save(booking);
        } catch (DataAccessException dae) {
            // Гонка: данные устарели между проверками выше и вставкой - реальную атомарную
            // проверку и её ошибку (P0001-P0003) выполнил триггер reserve_slot_inventory().
            throw SqlErrorMapper.toBusinessException(dae);
        }

        IdempotencyKey ik = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .clientId(clientId)
                .endpoint(CREATE_BOOKING_ENDPOINT)
                .requestFingerprint(fingerprint)
                .responseStatus((short) 201)
                .responseBody("{\"id\":\"" + saved.getId() + "\"}")
                .build();
        idempotencyKeyRepository.save(ik);

        return toBookingResponse(saved);
    }

    public BookingResponse cancelBooking(UUID bookingId, UUID clientId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found", HttpStatus.NOT_FOUND, "not_found"));

        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException("Доступ запрещён", HttpStatus.FORBIDDEN, "forbidden");
        }

        if (booking.getStatus() != BookingStatus.active) {
            throw new BusinessException("Бронь уже отменена", HttpStatus.CONFLICT, "already_cancelled");
        }

        if (OffsetDateTime.now().isAfter(booking.getSlot().getStartAt())) {
            throw new BusinessException("Заезд уже начался, операция недоступна", HttpStatus.UNPROCESSABLE_ENTITY, "slot_started");
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean early = now.plusHours(1).isBefore(booking.getSlot().getStartAt())
                || now.plusHours(1).isEqual(booking.getSlot().getStartAt());

        BookingStatus newStatus = early ? BookingStatus.cancelled : BookingStatus.late_cancel;
        booking.setStatus(newStatus);
        booking.setCancelledAt(now);

        if (managesInventoryInApplication()) {
            Slot slot = booking.getSlot();
            slot.setFreeKarts((short) (slot.getFreeKarts() + booking.getSeatsCount()));
            slot.setFreeRentalGear((short) (slot.getFreeRentalGear() + booking.getRentalGearCount()));
        }

        Booking updated;
        try {
            updated = bookingRepository.save(booking);
        } catch (DataAccessException dae) {
            throw SqlErrorMapper.toBusinessException(dae);
        }
        return toBookingResponse(updated);
    }

    public BookingListResponse listBookings(UUID clientId, List<BookingStatus> statuses, Integer limit, Integer offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "slot.startAt"));
        Page<Booking> page;
        if (statuses != null && !statuses.isEmpty()) {
            page = bookingRepository.findAllByClientIdAndStatusInWithSlot(clientId, statuses, pageable);
        } else {
            page = bookingRepository.findAllByClientIdWithSlot(clientId, pageable);
        }

        List<BookingSummaryResponse> items = page.getContent().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
        PaginationMeta meta = new PaginationMeta(limit, offset, page.getTotalElements());
        return new BookingListResponse(items, meta);
    }

    public BookingResponse getBooking(UUID bookingId, UUID clientId) {
        Booking booking = bookingRepository.findByIdWithSlot(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found", HttpStatus.NOT_FOUND, "not_found"));
        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException("Доступ запрещён", HttpStatus.FORBIDDEN, "forbidden");
        }
        return toBookingResponse(booking);
    }

    public void cancelSlotByCenter(UUID slotId, String reason) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException("Slot not found", HttpStatus.NOT_FOUND, "not_found"));
        slot.setStatus(SlotStatus.cancelled);
        slot.setCancellationReason(reason);
        slotRepository.save(slot);
        log.info("Slot {} cancelled by center with reason: {}", slotId, reason);
    }

    // ---- Вспомогательные методы ----

    private boolean managesInventoryInApplication() {
        return activeProfile == null || activeProfile.isBlank() || activeProfile.contains("dev") || activeProfile.contains("test");
    }

    private String buildFingerprint(CreateBookingRequest request) {
        return request.getSlotId() + ":" + request.getSeatsCount() + ":" + request.getRentalGearCount();
    }

    private BookingResponse toBookingResponse(Booking booking) {
        Slot slot = booking.getSlot();
        SlotResponse slotResponse = slotService.getSlot(slot.getId());

        RatingSummaryDto ratingDto = ratingRepository.findByBookingId(booking.getId())
                .map(r -> new RatingSummaryDto(r.getValue(), r.getComment()))
                .orElse(null);

        return new BookingResponse(
                booking.getId(),
                slot.getId(),
                booking.getClient().getId(),
                booking.getSeatsCount(),
                booking.getRentalGearCount(),
                booking.getStatus(),
                booking.getPriceTotal(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                slotResponse,
                ratingDto
        );
    }

    private BookingSummaryResponse toSummaryResponse(Booking booking) {
        Slot slot = booking.getSlot();
        SlotSummaryResponse slotSummary = slotService.toSummary(slot);

        RatingSummaryDto ratingDto = ratingRepository.findByBookingId(booking.getId())
                .map(r -> new RatingSummaryDto(r.getValue(), r.getComment()))
                .orElse(null);

        return new BookingSummaryResponse(
                booking.getId(),
                slot.getId(),
                booking.getSeatsCount(),
                booking.getRentalGearCount(),
                booking.getStatus(),
                booking.getPriceTotal(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                slotSummary,
                ratingDto
        );
    }
}