package ru.vsu.cs.yesikov.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vsu.cs.yesikov.model.enums.OtpPurpose;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.login;

    @Column(name = "code_hash", nullable = false, columnDefinition = "TEXT")
    private String codeHash;

    @Column(nullable = false)
    @Builder.Default
    private Short attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Short maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "resend_after", nullable = false)
    private OffsetDateTime resendAfter;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}