package ru.vsu.cs.yesikov.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.Booking;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Существующие методы (рабочие)
    @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId AND b.status IN :statuses")
    Page<Booking> findAllByClientIdAndStatusIn(@Param("clientId") UUID clientId,
                                               @Param("statuses") List<BookingStatus> statuses,
                                               Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId")
    Page<Booking> findAllByClientId(@Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.client.id = :clientId AND b.slot.id = :slotId AND b.status = 'active'")
    Optional<Booking> findActiveByClientAndSlot(@Param("clientId") UUID clientId, @Param("slotId") UUID slotId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.client.id = :clientId AND b.slot.id = :slotId AND b.status = 'active'")
    boolean existsActiveBookingForClientAndSlot(@Param("clientId") UUID clientId, @Param("slotId") UUID slotId);

    // НОВЫЕ методы с JOIN FETCH для оптимизации (устранение N+1)
    @Query("SELECT b FROM Booking b JOIN FETCH b.slot s JOIN FETCH s.trackConfig JOIN FETCH s.marshal WHERE b.id = :id")
    Optional<Booking> findByIdWithSlot(@Param("id") UUID id);

    @Query("SELECT b FROM Booking b JOIN FETCH b.slot s JOIN FETCH s.trackConfig JOIN FETCH s.marshal WHERE b.client.id = :clientId")
    Page<Booking> findAllByClientIdWithSlot(@Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN FETCH b.slot s JOIN FETCH s.trackConfig JOIN FETCH s.marshal WHERE b.client.id = :clientId AND b.status IN :statuses")
    Page<Booking> findAllByClientIdAndStatusInWithSlot(@Param("clientId") UUID clientId,
                                                       @Param("statuses") List<BookingStatus> statuses,
                                                       Pageable pageable);

    // Нативные запросы для push-уведомлений (без изменений)
    @Query(value = "SELECT b.* FROM bookings b " +
            "JOIN slots s ON b.slot_id = s.id " +
            "WHERE s.status = CAST(:slotStatus AS slot_status) " +
            "AND b.status = 'active' " +
            "AND NOT EXISTS (SELECT 1 FROM push_notifications_log l " +
            "                WHERE l.booking_id = b.id " +
            "                AND l.type = CAST(:type AS push_notification_type))",
            nativeQuery = true)
    List<Booking> findActiveBySlotStatusAndNotNotified(@Param("slotStatus") String slotStatus,
                                                       @Param("type") String type);

    @Query(value = "SELECT b.* FROM bookings b " +
            "JOIN slots s ON b.slot_id = s.id " +
            "WHERE b.status = 'active' " +
            "AND s.start_at BETWEEN :from AND :to " +
            "AND NOT EXISTS (SELECT 1 FROM push_notifications_log l " +
            "                WHERE l.booking_id = b.id " +
            "                AND l.type = CAST(:type AS push_notification_type))",
            nativeQuery = true)
    List<Booking> findActiveByStartTimeBetweenAndNotNotified(@Param("from") OffsetDateTime from,
                                                             @Param("to") OffsetDateTime to,
                                                             @Param("type") String type);

    @Query("SELECT b FROM Booking b WHERE b.idempotencyKey = :key AND b.client.id = :clientId")
    Optional<Booking> findByIdempotencyKeyAndClientId(@Param("key") UUID key, @Param("clientId") UUID clientId);
}