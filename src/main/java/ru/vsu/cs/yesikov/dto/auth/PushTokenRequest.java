package ru.vsu.cs.yesikov.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.vsu.cs.yesikov.model.enums.PushPlatform;

@Data
public class PushTokenRequest {
    @NotBlank
    @Size(max = 4096, message = "Token too long")
    private String token;

    @NotNull
    private PushPlatform platform;
}