package ru.vsu.cs.yesikov.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RequestCodeRequest {
    @NotBlank
    @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$", message = "Invalid phone format")
    private String phone;
}