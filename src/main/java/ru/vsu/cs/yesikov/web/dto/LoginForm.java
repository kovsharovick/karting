// ru.vsu.cs.yesikov.web.dto.LoginForm.java
package ru.vsu.cs.yesikov.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginForm {
    @NotBlank(message = "Введите номер телефона")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Введите корректный номер в формате +7XXXXXXXXXX")
    private String phone;
}