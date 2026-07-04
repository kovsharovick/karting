package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query("SELECT c FROM Client c WHERE c.phone = :phone AND c.deletedAt IS NULL")
    Optional<Client> findByPhone(@Param("phone") String phone);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);
}