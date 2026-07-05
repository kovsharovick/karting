package ru.vsu.cs.yesikov.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.dto.MarshalDto;
import ru.vsu.cs.yesikov.dto.TrackConfigurationDto;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;
import ru.vsu.cs.yesikov.dto.slot.SlotListResponse;
import ru.vsu.cs.yesikov.dto.slot.SlotResponse;
import ru.vsu.cs.yesikov.dto.slot.SlotSummaryResponse;
import ru.vsu.cs.yesikov.exception.BusinessException;
import ru.vsu.cs.yesikov.model.Slot;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;
import ru.vsu.cs.yesikov.repository.SlotRepository;


import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SlotService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final SlotRepository slotRepository;

    @Transactional(readOnly = true)
    public SlotListResponse listSlots(LocalDate dateFrom, LocalDate dateTo,
                                      List<TrackConfigType> trackConfigs,
                                      List<UUID> instructorIds,
                                      Boolean onlyAvailable,
                                      Integer limit, Integer offset) {
        // Если дата не указана – по умолчанию сегодня
        if (dateFrom == null) {
            dateFrom = LocalDate.now();
        }
        if (dateTo == null) {
            dateTo = dateFrom.plusDays(7);
        }

        // Преобразуем LocalDate в OffsetDateTime (UTC)
        OffsetDateTime from = dateFrom.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime to = dateTo.atTime(23, 59, 59, 999_999_999).atOffset(ZoneOffset.UTC);

        // Далее используем from и to как раньше
        Specification<Slot> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("startAt"), from, to)
        );

        if (trackConfigs != null && !trackConfigs.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("trackConfig").get("type").in(trackConfigs)
            );
        }
        if (instructorIds != null && !instructorIds.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("marshal").get("id").in(instructorIds)
            );
        }
        if (Boolean.TRUE.equals(onlyAvailable)) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("freeKarts"), 0)
            );
        }
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), SlotStatus.scheduled)
        );

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Slot> page = slotRepository.findAll(spec, pageable);

        // Инициализация ленивых полей
        page.getContent().forEach(slot -> {
            Hibernate.initialize(slot.getTrackConfig());
            Hibernate.initialize(slot.getMarshal());
        });

        List<SlotSummaryResponse> items = page.getContent().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        PaginationMeta meta = new PaginationMeta(limit, offset, page.getTotalElements());
        return new SlotListResponse(items, meta);
    }

    @Transactional(readOnly = true)
    public SlotResponse getSlot(UUID slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException("Slot not found", HttpStatus.NOT_FOUND, "not_found"));
        Hibernate.initialize(slot.getTrackConfig());
        Hibernate.initialize(slot.getMarshal());
        return toFullResponse(slot);
    }

    public SlotSummaryResponse toSummary(Slot slot) {
        return new SlotSummaryResponse(
                slot.getId(),
                new TrackConfigurationDto(slot.getTrackConfig().getId(), slot.getTrackConfig().getName(),
                        slot.getTrackConfig().getType(), slot.getTrackConfig().getDescription()),
                new MarshalDto(slot.getMarshal().getId(), slot.getMarshal().getName(), slot.getMarshal().getRatingAvg()),
                slot.getStartAt(),
                slot.getStartAt().format(DATE_FORMATTER),   // форматированная строка
                slot.getTotalKarts(),
                slot.getFreeKarts(),
                slot.getFreeRentalGear(),
                slot.getPriceKart(),
                slot.getPriceGearRental(),
                slot.getStatus(),
                slot.getMeetingPoint(),
                slot.getAddress(),
                slot.getDurationMinutes()
        );
    }

    private SlotResponse toFullResponse(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                new TrackConfigurationDto(slot.getTrackConfig().getId(), slot.getTrackConfig().getName(),
                        slot.getTrackConfig().getType(), slot.getTrackConfig().getDescription()),
                new MarshalDto(slot.getMarshal().getId(), slot.getMarshal().getName(), slot.getMarshal().getRatingAvg()),
                slot.getStartAt(),
                slot.getStartAt().format(DATE_FORMATTER),   // форматированная строка
                slot.getTotalKarts(),
                slot.getFreeKarts(),
                slot.getFreeRentalGear(),
                slot.getPriceKart(),
                slot.getPriceGearRental(),
                slot.getRequirementsText(),
                slot.getStatus(),
                slot.getMeetingPoint(),
                slot.getAddress(),
                slot.getDurationMinutes()
        );
    }
}