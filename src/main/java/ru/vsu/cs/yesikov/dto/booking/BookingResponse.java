package ru.vsu.cs.yesikov.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.RatingSummaryDto;
import ru.vsu.cs.yesikov.dto.slot.SlotResponse;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID slotId;
    private UUID clientId;
    private Short seatsCount;
    private Short rentalGearCount;
    private BookingStatus status;
    private Integer priceTotal;
    private String cancellationReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;
    private SlotResponse slot;        // вложенный
    private RatingSummaryDto rating;  // может быть null
}