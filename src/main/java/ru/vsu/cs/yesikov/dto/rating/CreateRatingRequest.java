package ru.vsu.cs.yesikov.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRatingRequest {
    @NotNull
    private UUID marshalId;

    @NotNull
    private UUID bookingId;

    @NotNull
    @Min(1)
    @Max(5)
    private Short value;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}