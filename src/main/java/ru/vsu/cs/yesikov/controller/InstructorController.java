package ru.vsu.cs.yesikov.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vsu.cs.yesikov.dto.MarshalDto;
import ru.vsu.cs.yesikov.dto.common.PaginationMeta;
import ru.vsu.cs.yesikov.dto.instructor.InstructorListResponse;
import ru.vsu.cs.yesikov.model.Marshal;
import ru.vsu.cs.yesikov.repository.MarshalRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/instructors")
@RequiredArgsConstructor
public class InstructorController {

    private final MarshalRepository marshalRepository;

    @GetMapping
    public ResponseEntity<InstructorListResponse> listInstructors(
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        Page<Marshal> page = marshalRepository.findAll(PageRequest.of(offset / limit, limit));
        List<MarshalDto> items = page.getContent().stream()
                .map(m -> new MarshalDto(m.getId(), m.getName(), m.getRatingAvg()))
                .collect(Collectors.toList());
        PaginationMeta meta = new PaginationMeta(limit, offset, page.getTotalElements());
        return ResponseEntity.ok(new InstructorListResponse(items, meta));
    }
}