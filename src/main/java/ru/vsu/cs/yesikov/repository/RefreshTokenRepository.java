package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.RefreshToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :hash AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    Optional<RefreshToken> findActiveByHash(@Param("hash") String hash, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.client.id = :clientId AND rt.revokedAt IS NULL")
    void revokeAllForClient(@Param("clientId") UUID clientId, @Param("now") OffsetDateTime now);

    // Новый метод для корректной проверки refresh-токена через matches()
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.client.id = :clientId AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    List<RefreshToken> findAllByClientIdAndRevokedAtIsNullAndExpiresAtAfter(@Param("clientId") UUID clientId,
                                                                            @Param("now") OffsetDateTime now);
}