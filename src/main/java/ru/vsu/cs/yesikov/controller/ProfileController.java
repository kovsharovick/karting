package ru.vsu.cs.yesikov.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.ClientDto;
import ru.vsu.cs.yesikov.dto.profile.ChangePhoneConfirmRequest;
import ru.vsu.cs.yesikov.dto.profile.ChangePhoneRequestCodeRequest;
import ru.vsu.cs.yesikov.dto.profile.UpdateProfileRequest;
import ru.vsu.cs.yesikov.service.ProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ClientDto> getProfile(@AuthenticationPrincipal UUID clientId) {
        return ResponseEntity.ok(profileService.getProfile(clientId));
    }

    @PatchMapping
    public ResponseEntity<ClientDto> updateProfile(@AuthenticationPrincipal UUID clientId,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(clientId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UUID clientId) {
        profileService.deleteAccount(clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/request-code")
    public ResponseEntity<Void> requestPhoneChangeCode(@AuthenticationPrincipal UUID clientId,
                                                       @Valid @RequestBody ChangePhoneRequestCodeRequest request) {
        profileService.requestPhoneChangeCode(clientId, request.getNewPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/confirm")
    public ResponseEntity<ClientDto> confirmPhoneChange(@AuthenticationPrincipal UUID clientId,
                                                        @Valid @RequestBody ChangePhoneConfirmRequest request) {
        return ResponseEntity.ok(profileService.confirmPhoneChange(clientId, request));
    }
}