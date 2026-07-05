// ru.vsu.cs.yesikov.web.WebController.java
package ru.vsu.cs.yesikov.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.auth.VerifyCodeResponse;
import ru.vsu.cs.yesikov.dto.booking.BookingResponse;
import ru.vsu.cs.yesikov.dto.booking.CreateBookingRequest;
import ru.vsu.cs.yesikov.dto.rating.CreateRatingRequest;
import ru.vsu.cs.yesikov.dto.slot.SlotResponse;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;
import ru.vsu.cs.yesikov.service.AuthService;
import ru.vsu.cs.yesikov.service.BookingService;
import ru.vsu.cs.yesikov.service.RatingService;
import ru.vsu.cs.yesikov.service.SlotService;
import ru.vsu.cs.yesikov.web.dto.BookingForm;
import ru.vsu.cs.yesikov.web.dto.LoginForm;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AuthService authService;
    private final SlotService slotService;
    private final BookingService bookingService;
    private final RatingService ratingService;

    // ===== АВТОРИЗАЦИЯ (без изменений) =====

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/request-code")
    public String requestCode(@Valid @ModelAttribute("loginForm") LoginForm form,
                              BindingResult result,
                              HttpSession session,
                              Model model) {
        if (result.hasErrors()) {
            return "login";
        }
        try {
            authService.requestCode(form.getPhone());
            session.setAttribute("phone", form.getPhone());
            model.addAttribute("phone", form.getPhone());
            return "login-code";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка отправки кода: " + e.getMessage());
            model.addAttribute("loginForm", form);
            return "login";
        }
    }

    @PostMapping("/verify-code")
    public String verifyCode(@RequestParam String code,
                             HttpSession session,
                             Model model) {
        String phone = (String) session.getAttribute("phone");
        if (phone == null) {
            return "redirect:/login";
        }
        try {
            VerifyCodeResponse response = authService.verifyCode(phone, code);
            session.setAttribute("clientId", response.getClient().getId());
            session.setAttribute("client", response.getClient());
            session.setAttribute("isNew", response.getIsNew());
            return "redirect:/slots";
        } catch (Exception e) {
            model.addAttribute("phone", phone);
            model.addAttribute("error", "Неверный код. Проверьте и введите ещё раз.");
            return "login-code";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        UUID clientId = (UUID) session.getAttribute("clientId");
        if (clientId != null) {
            authService.logout(clientId);
        }
        session.invalidate();
        return "redirect:/login";
    }

    // ===== СЛОТЫ (без изменений) =====

    @GetMapping("/slots")
    public String slotList(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                           @RequestParam(required = false) List<TrackConfigType> trackConfig,
                           @RequestParam(required = false) List<UUID> instructorId,
                           @RequestParam(required = false) Boolean onlyAvailable,
                           Model model,
                           HttpSession session) {
        if (session.getAttribute("clientId") == null) {
            return "redirect:/login";
        }
        var slots = slotService.listSlots(dateFrom, dateTo, trackConfig, instructorId, onlyAvailable, 100, 0);
        model.addAttribute("slots", slots.getItems());
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterTrackConfig", trackConfig);
        model.addAttribute("filterInstructor", instructorId);
        model.addAttribute("filterOnlyAvailable", onlyAvailable);
        return "slot-list";
    }

    @GetMapping("/slots/{slotId}")
    public String slotCard(@PathVariable UUID slotId, Model model, HttpSession session) {
        if (session.getAttribute("clientId") == null) {
            return "redirect:/login";
        }
        SlotResponse slot = slotService.getSlot(slotId);
        model.addAttribute("slot", slot);
        return "slot-card";
    }

    // ===== БРОНИРОВАНИЕ (добавлен префикс /web) =====

    @GetMapping("/booking/{slotId}")
    public String bookingForm(@PathVariable UUID slotId, Model model, HttpSession session) {
        if (session.getAttribute("clientId") == null) {
            return "redirect:/login";
        }
        SlotResponse slot = slotService.getSlot(slotId);
        model.addAttribute("slot", slot);
        model.addAttribute("bookingForm", new BookingForm());
        return "booking";
    }

    @PostMapping("/booking/{slotId}")
    public String createBooking(@PathVariable UUID slotId,
                                @Valid @ModelAttribute("bookingForm") BookingForm form,
                                BindingResult result,
                                HttpSession session,
                                Model model) {
        UUID clientId = (UUID) session.getAttribute("clientId");
        if (clientId == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            SlotResponse slot = slotService.getSlot(slotId);
            model.addAttribute("slot", slot);
            return "booking";
        }
        try {
            CreateBookingRequest request = new CreateBookingRequest();
            request.setSlotId(slotId);
            request.setSeatsCount(form.getSeatsCount().shortValue());
            request.setRentalGearCount(form.getRentalGearCount().shortValue());
            String idempotencyKey = UUID.randomUUID().toString();
            BookingResponse booking = bookingService.createBooking(clientId, request, UUID.fromString(idempotencyKey));
            return "redirect:/booking-success/" + booking.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка записи: " + e.getMessage());
            SlotResponse slot = slotService.getSlot(slotId);
            model.addAttribute("slot", slot);
            return "booking";
        }
    }

    @GetMapping("/booking-success/{bookingId}")
    public String bookingSuccess(@PathVariable UUID bookingId, Model model, HttpSession session) {
        if (session.getAttribute("clientId") == null) {
            return "redirect:/login";
        }
        UUID clientId = (UUID) session.getAttribute("clientId");
        BookingResponse booking = bookingService.getBooking(bookingId, clientId);
        model.addAttribute("booking", booking);
        return "booking-success";
    }

    // ===== МОИ БРОНИ =====

    @GetMapping("/bookings")
    public String myBookings(@RequestParam(required = false) List<BookingStatus> status,
                             Model model,
                             HttpSession session) {
        UUID clientId = (UUID) session.getAttribute("clientId");
        if (clientId == null) {
            return "redirect:/login";
        }
        var bookings = bookingService.listBookings(clientId, status, 100, 0);
        model.addAttribute("bookings", bookings.getItems());
        return "my-bookings";
    }

    @GetMapping("/bookings/{bookingId}")
    public String bookingDetails(@PathVariable UUID bookingId, Model model, HttpSession session) {
        if (session.getAttribute("clientId") == null) {
            return "redirect:/login";
        }
        UUID clientId = (UUID) session.getAttribute("clientId");
        BookingResponse booking = bookingService.getBooking(bookingId, clientId);
        OffsetDateTime now = OffsetDateTime.now();
        boolean canCancel = booking.getStatus() == BookingStatus.active
                && booking.getSlot() != null
                && booking.getSlot().getStartAt() != null
                && booking.getSlot().getStartAt().isAfter(now);
        boolean canRate = booking.getStatus() == BookingStatus.active
                && booking.getRating() == null
                && booking.getSlot() != null
                && booking.getSlot().getStartAt() != null
                && booking.getSlot().getDurationMinutes() != null
                && booking.getSlot().getStartAt().plusMinutes(booking.getSlot().getDurationMinutes()).isBefore(now);

        model.addAttribute("booking", booking);
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("canRate", canRate);
        return "booking-details";
    }

    // ===== ОТМЕНА (AJAX) – изменён путь =====

    @PostMapping("/web/bookings/{bookingId}/cancel")
    @ResponseBody
    public String cancelBooking(@PathVariable("bookingId") UUID bookingId,
                                @RequestParam String confirm,
                                HttpSession session) {
        UUID clientId = (UUID) session.getAttribute("clientId");
        if (clientId == null) {
            return "error: not authenticated";
        }
        if ("true".equals(confirm)) {
            try {
                bookingService.cancelBooking(bookingId, clientId);
                return "success";
            } catch (Exception e) {
                return "error: " + e.getMessage();
            }
        }
        return "cancelled";
    }

    // ===== ОЦЕНКА (AJAX) – изменён путь =====

    @PostMapping("/web/ratings")
    @ResponseBody
    public String createRating(@RequestBody CreateRatingRequest request,
                               HttpSession session) {
        UUID clientId = (UUID) session.getAttribute("clientId");
        if (clientId == null) {
            return "error: not authenticated";
        }
        try {
            ratingService.createRating(clientId, request);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}