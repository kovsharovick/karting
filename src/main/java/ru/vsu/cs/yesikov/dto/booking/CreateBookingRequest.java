package ru.vsu.cs.yesikov.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull
    private UUID slotId;

    @Min(1)
    private Short seatsCount;

    @Min(0)
    private Short rentalGearCount;
}