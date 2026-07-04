package ru.vsu.cs.yesikov.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;

import java.util.List;

@Data
@AllArgsConstructor
public class BookingListResponse {
    private List<BookingSummaryResponse> items;
    private PaginationMeta meta;
}