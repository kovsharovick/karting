package ru.vsu.cs.yesikov.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Id
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Id
    @Column(nullable = false, length = 100)
    private String endpoint;

    @Column(name = "request_fingerprint", nullable = false, columnDefinition = "TEXT")
    private String requestFingerprint;

    @Column(name = "response_status")
    private Short responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")  // было JSONB
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}