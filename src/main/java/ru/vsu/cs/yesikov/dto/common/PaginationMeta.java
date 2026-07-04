package ru.vsu.cs.yesikov.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaginationMeta {
    private Integer limit;
    private Integer offset;
    private Long total;
}