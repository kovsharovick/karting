package ru.vsu.cs.yesikov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingSummaryDto {
    private Short value;
    private String comment;
}