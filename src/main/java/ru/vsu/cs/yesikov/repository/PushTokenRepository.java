package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.PushToken;
import ru.vsu.cs.yesikov.model.enums.PushPlatform;

import java.util.List;
import java.util.UUID;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {
    List<PushToken> findAllByClientId(UUID clientId);

    @Modifying
    @Query("DELETE FROM PushToken pt WHERE pt.client.id = :clientId AND pt.token = :token AND pt.platform = :platform")
    void deleteByClientIdAndTokenAndPlatform(@Param("clientId") UUID clientId,
                                             @Param("token") String token,
                                             @Param("platform") PushPlatform platform);
}