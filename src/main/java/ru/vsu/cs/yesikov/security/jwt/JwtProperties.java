package ru.vsu.cs.yesikov.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Integer accessTokenTtl;  // seconds
    private Integer refreshTokenTtl; // seconds
}