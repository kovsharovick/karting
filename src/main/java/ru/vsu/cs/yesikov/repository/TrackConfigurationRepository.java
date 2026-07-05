package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.vsu.cs.yesikov.model.TrackConfiguration;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;

import java.util.Optional;
import java.util.UUID;

public interface TrackConfigurationRepository extends JpaRepository<TrackConfiguration, UUID> {
    Optional<TrackConfiguration> findByType(TrackConfigType type);
}