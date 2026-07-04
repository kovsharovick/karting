package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.PushNotificationLog;
import ru.vsu.cs.yesikov.model.enums.PushNotificationType;

import java.util.UUID;

public interface PushNotificationLogRepository extends JpaRepository<PushNotificationLog, UUID> {

    @Query("SELECT COUNT(l) > 0 FROM PushNotificationLog l " +
            "WHERE l.booking.id = :bookingId AND l.type = :type")
    boolean existsByBookingAndType(@Param("bookingId") UUID bookingId,
                                   @Param("type") PushNotificationType type);
}