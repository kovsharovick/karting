package ru.vsu.cs.yesikov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.vsu.cs.yesikov.model.Slot;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID>, JpaSpecificationExecutor<Slot> {

    @Query("SELECT s FROM Slot s WHERE s.status = :status AND s.startAt > :now ORDER BY s.startAt")
    List<Slot> findUpcomingSlots(@Param("status") SlotStatus status, @Param("now") OffsetDateTime now);
}