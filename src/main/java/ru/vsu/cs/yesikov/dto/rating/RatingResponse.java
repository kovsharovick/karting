package ru.vsu.cs.yesikov.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class RatingResponse {
    private UUID id;
    private UUID clientId;
    private UUID marshalId;
    private UUID bookingId;
    private Short value;
    private String comment;
    private OffsetDateTime createdAt;
}