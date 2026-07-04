package ru.vsu.cs.yesikov.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank
    @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{4,6}$")
    private String code;
}