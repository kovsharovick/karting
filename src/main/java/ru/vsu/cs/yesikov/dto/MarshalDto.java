package ru.vsu.cs.yesikov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class MarshalDto {
    private UUID id;
    private String name;
    private BigDecimal ratingAvg;
}