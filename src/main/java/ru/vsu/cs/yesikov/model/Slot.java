package ru.vsu.cs.yesikov.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_config_id", nullable = false)
    private TrackConfiguration trackConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marshal_id", nullable = false)
    private Marshal marshal;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Short durationMinutes = 20;

    @Column(name = "total_karts", nullable = false)
    private Short totalKarts;

    @Column(name = "free_karts", nullable = false, insertable = false, updatable = false)
    private Short freeKarts;

    @Column(name = "total_rental_gear", nullable = false)
    @Builder.Default
    private Short totalRentalGear = 0;

    @Column(name = "free_rental_gear", nullable = false, insertable = false, updatable = false)
    private Short freeRentalGear;

    @Column(name = "price_kart", nullable = false)
    private Integer priceKart;

    @Column(name = "price_gear_rental", nullable = false)
    private Integer priceGearRental;

    @Column(name = "requirements_text", columnDefinition = "TEXT")
    private String requirementsText;

    @Column(name = "meeting_point", columnDefinition = "TEXT")
    private String meetingPoint;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SlotStatus status = SlotStatus.scheduled;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}