package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsu.cs.yesikov.model.Marshal;

import java.util.List;
import java.util.UUID;

public interface MarshalRepository extends JpaRepository<Marshal, UUID> {
    List<Marshal> findAllByIsActiveTrue();
}