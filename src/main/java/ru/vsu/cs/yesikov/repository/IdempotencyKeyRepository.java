package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsu.cs.yesikov.model.IdempotencyKey;
import ru.vsu.cs.yesikov.model.IdempotencyKeyId;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {

    Optional<IdempotencyKey> findByIdempotencyKeyAndClientIdAndEndpoint(UUID key, UUID clientId, String endpoint);
}