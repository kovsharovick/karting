package ru.vsu.cs.yesikov.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.yesikov.dto.auth.*;
import ru.vsu.cs.yesikov.service.AuthService;
import ru.vsu.cs.yesikov.service.PushService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PushService pushService;

    @PostMapping("/request-code")
    public ResponseEntity<Void> requestCode(@Valid @RequestBody RequestCodeRequest request) {
        authService.requestCode(request.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<VerifyCodeResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(authService.verifyCode(request.getPhone(), request.getCode()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID clientId) {
        authService.logout(clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push-tokens")
    public ResponseEntity<Void> registerPush(@AuthenticationPrincipal UUID clientId,
                                             @Valid @RequestBody PushTokenRequest request) {
        pushService.registerPushToken(clientId, request.getToken(), request.getPlatform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push-tokens")
    public ResponseEntity<Void> deletePush(@AuthenticationPrincipal UUID clientId,
                                           @Valid @RequestBody PushTokenRequest request) {
        pushService.deletePushToken(clientId, request.getToken(), request.getPlatform());
        return ResponseEntity.noContent().build();
    }
}