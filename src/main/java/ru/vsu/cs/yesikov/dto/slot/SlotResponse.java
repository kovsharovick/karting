package ru.vsu.cs.yesikov.dto.slot;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.MarshalDto;
import ru.vsu.cs.yesikov.dto.TrackConfigurationDto;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SlotResponse {
    private UUID id;
    private TrackConfigurationDto trackConfig;
    private MarshalDto marshal;
    private OffsetDateTime startAt;
    private String startAtFormatted;  // <-- новое поле
    private Short totalKarts;
    private Short freeKarts;
    private Short freeRentalGear;
    private Integer priceKart;
    private Integer priceGearRental;
    private String requirementsText;
    private SlotStatus status;
    private String meetingPoint;
    private String address;
    private Short durationMinutes;
}