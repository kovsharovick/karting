package ru.vsu.cs.yesikov.dto.profile;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePhoneConfirmRequest {
    @Pattern(regexp = "^\\+[1-9][0-9]{1,14}$")
    private String newPhone;

    @Pattern(regexp = "^\\d{4,6}$")
    private String code;
}