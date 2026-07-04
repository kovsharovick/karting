package ru.vsu.cs.yesikov.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vsu.cs.yesikov.model.enums.PushNotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_notifications_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PushNotificationType type;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private OffsetDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = OffsetDateTime.now();
    }
}