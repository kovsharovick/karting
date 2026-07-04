package ru.vsu.cs.yesikov.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.rating.CreateRatingRequest;
import ru.vsu.cs.yesikov.dto.rating.RatingResponse;
import ru.vsu.cs.yesikov.service.RatingService;

import java.util.UUID;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal UUID clientId,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(clientId, request));
    }

    @GetMapping
    public ResponseEntity<RatingResponse> getRatingByBooking(
            @RequestParam UUID booking_id
    ) {
        return ResponseEntity.ok(ratingService.getRatingByBooking(booking_id));
    }
}