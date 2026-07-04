package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsu.cs.yesikov.model.Rating;

import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);
}