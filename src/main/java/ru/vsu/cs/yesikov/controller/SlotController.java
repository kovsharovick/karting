package ru.vsu.cs.yesikov.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.slot.SlotListResponse;
import ru.vsu.cs.yesikov.dto.slot.SlotResponse;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;
import ru.vsu.cs.yesikov.service.SlotService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping
    public ResponseEntity<SlotListResponse> listSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<TrackConfigType> trackConfig,
            @RequestParam(required = false) List<UUID> instructorId,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(slotService.listSlots(dateFrom, dateTo, trackConfig, instructorId, onlyAvailable, limit, offset));
    }

    @GetMapping("/{slotId}")
    public ResponseEntity<SlotResponse> getSlot(@PathVariable UUID slotId) {
        return ResponseEntity.ok(slotService.getSlot(slotId));
    }
}