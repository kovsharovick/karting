package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.OtpCode;
import ru.vsu.cs.yesikov.model.enums.OtpPurpose;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.purpose = :purpose AND o.consumedAt IS NULL AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpCode> findActiveByPhoneAndPurpose(@Param("phone") String phone,
                                                  @Param("purpose") OtpPurpose purpose,
                                                  @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE OtpCode o SET o.consumedAt = :now WHERE o.id = :id")
    void consumeCode(@Param("id") UUID id, @Param("now") OffsetDateTime now);

    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.consumedAt IS NULL")
    List<OtpCode> findAllActiveByPhone(@Param("phone") String phone);

    @Modifying
    @Query("UPDATE OtpCode o SET o.consumedAt = :now WHERE o.phone = :phone AND o.purpose = :purpose AND o.consumedAt IS NULL")
    int consumeActiveByPhoneAndPurpose(@Param("phone") String phone,
                                       @Param("purpose") OtpPurpose purpose,
                                       @Param("now") OffsetDateTime now);

    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.purpose = :purpose AND o.consumedAt IS NULL AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    Optional<OtpCode> findTopActiveByPhoneAndPurpose(
            @Param("phone") String phone,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") OffsetDateTime now
    );
}