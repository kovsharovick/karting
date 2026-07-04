package ru.vsu.cs.yesikov.dto.instructor;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.MarshalDto;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;

import java.util.List;

@Data
@AllArgsConstructor
public class InstructorListResponse {
    private List<MarshalDto> items;
    private PaginationMeta meta;
}