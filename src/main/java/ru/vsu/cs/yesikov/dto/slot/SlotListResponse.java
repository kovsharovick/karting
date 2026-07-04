package ru.vsu.cs.yesikov.dto.slot;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;

import java.util.List;

@Data
@AllArgsConstructor
public class SlotListResponse {
    private List<SlotSummaryResponse> items;
    private PaginationMeta meta;
}