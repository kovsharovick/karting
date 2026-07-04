package ru.vsu.cs.yesikov.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.dto.rating.CreateRatingRequest;
import ru.vsu.cs.yesikov.dto.rating.RatingResponse;
import ru.vsu.cs.yesikov.exception.BusinessException;
import ru.vsu.cs.yesikov.model.Booking;
import ru.vsu.cs.yesikov.model.Client;
import ru.vsu.cs.yesikov.model.Marshal;
import ru.vsu.cs.yesikov.model.Rating;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.repository.BookingRepository;
import ru.vsu.cs.yesikov.repository.ClientRepository;
import ru.vsu.cs.yesikov.repository.MarshalRepository;
import ru.vsu.cs.yesikov.repository.RatingRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final MarshalRepository marshalRepository;

    public RatingResponse createRating(UUID clientId, CreateRatingRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client not found", HttpStatus.NOT_FOUND, "not_found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking not found", HttpStatus.NOT_FOUND, "not_found"));

        // Проверка принадлежности клиенту
        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException("Доступ запрещён", HttpStatus.FORBIDDEN, "forbidden");
        }

        // Проверка статуса брони (должна быть active)
        if (booking.getStatus() != BookingStatus.active) {
            throw new BusinessException("Бронь не активна — оценка недоступна", HttpStatus.FORBIDDEN, "forbidden");
        }

        // Проверка, что заезд завершён (сравниваем с серверным временем)
        OffsetDateTime completesAt = booking.getSlot().getStartAt()
                .plusMinutes(booking.getSlot().getDurationMinutes());
        if (OffsetDateTime.now().isBefore(completesAt)) {
            throw new BusinessException("Заезд ещё не завершён", HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable");
        }

        // Проверка, что маршал соответствует брони
        if (!booking.getSlot().getMarshal().getId().equals(request.getMarshalId())) {
            throw new BusinessException("Маршал не соответствует брони", HttpStatus.BAD_REQUEST, "bad_request");
        }

        // Проверка, что оценка ещё не оставлена (уникальность booking_id)
        if (ratingRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException("Оценка уже оставлена", HttpStatus.CONFLICT, "conflict");
        }

        Marshal marshal = marshalRepository.findById(request.getMarshalId())
                .orElseThrow(() -> new BusinessException("Marshal not found", HttpStatus.NOT_FOUND, "not_found"));

        Rating rating = Rating.builder()
                .client(client)
                .booking(booking)
                .marshal(marshal)
                .value(request.getValue())
                .comment(request.getComment())
                .build();

        Rating saved = ratingRepository.save(rating);

        return toRatingResponse(saved);
    }

    public RatingResponse getRatingByBooking(UUID bookingId) {
        Rating rating = ratingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("Rating not found", HttpStatus.NOT_FOUND, "not_found"));
        return toRatingResponse(rating);
    }

    private RatingResponse toRatingResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getClient().getId(),
                rating.getMarshal().getId(),
                rating.getBooking().getId(),
                rating.getValue(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }
}