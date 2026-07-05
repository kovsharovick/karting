package ru.vsu.cs.yesikov.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.booking.BookingListResponse;
import ru.vsu.cs.yesikov.dto.booking.BookingResponse;
import ru.vsu.cs.yesikov.dto.booking.CreateBookingRequest;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.service.BookingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UUID clientId,
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(clientId, request, idempotencyKey));
    }

    @GetMapping
    public ResponseEntity<BookingListResponse> listBookings(
            @AuthenticationPrincipal UUID clientId,
            @RequestParam(required = false) List<BookingStatus> status,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(bookingService.listBookings(clientId, status, limit, offset));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @AuthenticationPrincipal UUID clientId,
            @PathVariable UUID bookingId
    ) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId, clientId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @AuthenticationPrincipal UUID clientId,
            @PathVariable UUID bookingId
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, clientId));
    }
}