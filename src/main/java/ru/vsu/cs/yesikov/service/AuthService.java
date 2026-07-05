package ru.vsu.cs.yesikov.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.dto.ClientDto;
import ru.vsu.cs.yesikov.dto.auth.TokenPairResponse;
import ru.vsu.cs.yesikov.dto.auth.VerifyCodeResponse;
import ru.vsu.cs.yesikov.exception.BusinessException;
import ru.vsu.cs.yesikov.model.Client;
import ru.vsu.cs.yesikov.model.OtpCode;
import ru.vsu.cs.yesikov.model.RefreshToken;
import ru.vsu.cs.yesikov.model.enums.OtpPurpose;
import ru.vsu.cs.yesikov.repository.ClientRepository;
import ru.vsu.cs.yesikov.repository.OtpCodeRepository;
import ru.vsu.cs.yesikov.repository.RefreshTokenRepository;
import ru.vsu.cs.yesikov.security.jwt.JwtUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final ClientRepository clientRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public void requestCode(String phone) {
        OffsetDateTime now = OffsetDateTime.now();
        otpCodeRepository.consumeActiveByPhoneAndPurpose(phone, OtpPurpose.login, now);

        String code = String.format("%04d", (int) (Math.random() * 10000));
        String hash = passwordEncoder.encode(code);
        OtpCode otp = OtpCode.builder()
                .phone(phone)
                .purpose(OtpPurpose.login)
                .codeHash(hash)
                .expiresAt(now.plusMinutes(5))
                .resendAfter(now.plusSeconds(60))
                .build();
        otpCodeRepository.save(otp);
        log.info("OTP for {}: {}", phone, code);
    }

    public VerifyCodeResponse verifyCode(String phone, String code) {
        // ---- Тестовый режим (только для разработки!) ----
        if ("test".equals(activeProfile) && "1234".equals(code)) {
            log.warn("Test mode: bypassing OTP verification for phone {}", phone);
            Client client = clientRepository.findByPhone(phone)
                    .orElseGet(() -> {
                        Client newClient = Client.builder()
                                .phone(phone)
                                .name("")
                                .build();
                        return clientRepository.save(newClient);
                    });
            String accessToken = jwtUtils.generateAccessToken(client.getId(), phone);
            String refreshToken = jwtUtils.generateRefreshToken(client.getId());

            String refreshHash = passwordEncoder.encode(refreshToken);
            RefreshToken rt = RefreshToken.builder()
                    .client(client)
                    .tokenHash(refreshHash)
                    .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenTtl()))
                    .build();
            refreshTokenRepository.save(rt);

            TokenPairResponse tokens = new TokenPairResponse(accessToken, refreshToken, "Bearer", jwtUtils.getAccessTokenTtl());
            ClientDto clientDto = new ClientDto(client.getId(), client.getName(), client.getPhone(), client.getCreatedAt());
            boolean isNew = client.getName() == null || client.getName().isEmpty();
            return new VerifyCodeResponse(tokens, clientDto, isNew);
        }
        // ---- Конец тестового режима ----

        OffsetDateTime now = OffsetDateTime.now();
        OtpCode otp = otpCodeRepository.findTopActiveByPhoneAndPurpose(phone, OtpPurpose.login, now)
                .orElseThrow(() -> new BusinessException("Неверный код или истёк срок", HttpStatus.BAD_REQUEST, "invalid_code"));

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts((short) (otp.getAttempts() + 1));
            otpCodeRepository.save(otp);
            throw new BusinessException("Неверный код. Проверьте и введите ещё раз", HttpStatus.BAD_REQUEST, "invalid_code");
        }

        otp.setConsumedAt(now);
        otpCodeRepository.save(otp);

        Client client = clientRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Client newClient = Client.builder()
                            .phone(phone)
                            .name("")
                            .build();
                    return clientRepository.save(newClient);
                });

        String accessToken = jwtUtils.generateAccessToken(client.getId(), phone);
        String refreshToken = jwtUtils.generateRefreshToken(client.getId());

        String refreshHash = passwordEncoder.encode(refreshToken);
        RefreshToken rt = RefreshToken.builder()
                .client(client)
                .tokenHash(refreshHash)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenTtl()))
                .build();
        refreshTokenRepository.save(rt);

        TokenPairResponse tokens = new TokenPairResponse(accessToken, refreshToken, "Bearer", jwtUtils.getAccessTokenTtl());
        ClientDto clientDto = new ClientDto(client.getId(), client.getName(), client.getPhone(), client.getCreatedAt());
        boolean isNew = client.getName() == null || client.getName().isEmpty();
        return new VerifyCodeResponse(tokens, clientDto, isNew);
    }

    /**
     * Обновление сессии (ротация refresh-токена) с корректной проверкой через passwordEncoder.matches()
     */
    public TokenPairResponse refreshToken(String refreshToken) {
        // Извлекаем clientId из JWT (без проверки подписи – она уже проверена в JwtUtils.extractClaims)
        Claims claims = jwtUtils.extractClaims(refreshToken);
        UUID clientId = UUID.fromString(claims.getSubject());
        OffsetDateTime now = OffsetDateTime.now();

        // Находим все активные токены клиента
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByClientIdAndRevokedAtIsNullAndExpiresAtAfter(clientId, now);
        RefreshToken found = null;
        for (RefreshToken rt : activeTokens) {
            if (passwordEncoder.matches(refreshToken, rt.getTokenHash())) {
                found = rt;
                break;
            }
        }

        if (found == null) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        // Ротация: помечаем старый как revoked
        found.setRevokedAt(now);
        refreshTokenRepository.save(found);

        Client client = found.getClient();
        String newAccess = jwtUtils.generateAccessToken(client.getId(), client.getPhone());
        String newRefresh = jwtUtils.generateRefreshToken(client.getId());

        String newRefreshHash = passwordEncoder.encode(newRefresh);
        RefreshToken newRt = RefreshToken.builder()
                .client(client)
                .tokenHash(newRefreshHash)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenTtl()))
                .build();
        refreshTokenRepository.save(newRt);

        return new TokenPairResponse(newAccess, newRefresh, "Bearer", jwtUtils.getAccessTokenTtl());
    }

    public void logout(UUID clientId) {
        refreshTokenRepository.revokeAllForClient(clientId, OffsetDateTime.now());
        log.info("Client {} logged out", clientId);
    }
}