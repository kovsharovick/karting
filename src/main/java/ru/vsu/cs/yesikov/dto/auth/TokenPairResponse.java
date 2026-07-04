package ru.vsu.cs.yesikov.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Integer expiresIn; // seconds
}