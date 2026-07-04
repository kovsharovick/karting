package ru.vsu.cs.yesikov.dto.profile;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePhoneRequestCodeRequest {
    @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$", message = "Invalid phone format")
    private String newPhone;
}