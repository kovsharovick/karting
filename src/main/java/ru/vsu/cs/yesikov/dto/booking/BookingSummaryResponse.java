package ru.vsu.cs.yesikov.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.RatingSummaryDto;
import ru.vsu.cs.yesikov.dto.slot.SlotSummaryResponse;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BookingSummaryResponse {
    private UUID id;
    private UUID slotId;
    private Short seatsCount;
    private Short rentalGearCount;
    private BookingStatus status;
    private Integer priceTotal;
    private String cancellationReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;
    private SlotSummaryResponse slot;
    private RatingSummaryDto rating;
}