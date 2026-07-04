package ru.vsu.cs.yesikov.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.dto.ClientDto;
import ru.vsu.cs.yesikov.dto.profile.ChangePhoneConfirmRequest;
import ru.vsu.cs.yesikov.dto.profile.UpdateProfileRequest;
import ru.vsu.cs.yesikov.exception.BusinessException;
import ru.vsu.cs.yesikov.model.Booking;
import ru.vsu.cs.yesikov.model.Client;
import ru.vsu.cs.yesikov.model.OtpCode;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.model.enums.OtpPurpose;
import ru.vsu.cs.yesikov.repository.BookingRepository;
import ru.vsu.cs.yesikov.repository.ClientRepository;
import ru.vsu.cs.yesikov.repository.OtpCodeRepository;
import ru.vsu.cs.yesikov.repository.RefreshTokenRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProfileService {

    private final ClientRepository clientRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;

    public ClientDto getProfile(UUID clientId) {
        Client client = findClient(clientId);
        return toClientDto(client);
    }

    public ClientDto updateProfile(UUID clientId, UpdateProfileRequest request) {
        Client client = findClient(clientId);
        client.setName(request.getName());
        Client saved = clientRepository.save(client);
        return toClientDto(saved);
    }

    public void deleteAccount(UUID clientId) {
        Client client = findClient(clientId);
        // Soft delete
        client.setDeletedAt(OffsetDateTime.now());
        clientRepository.save(client);

        // Отмена всех активных броней клиента
        List<Booking> activeBookings = bookingRepository.findAllByClientId(clientId, Pageable.unpaged()).getContent();
        for (Booking booking : activeBookings) {
            if (booking.getStatus() == BookingStatus.active) {
                // Отменяем бронь (освобождаем инвентарь, если заезд ещё не начался)
                // Используем ту же логику, что и в cancelBooking
                OffsetDateTime now = OffsetDateTime.now();
                boolean early = now.plusHours(1).isBefore(booking.getSlot().getStartAt()) || now.plusHours(1).equals(booking.getSlot().getStartAt());
                BookingStatus newStatus = early ? BookingStatus.cancelled : BookingStatus.late_cancel;
                booking.setStatus(newStatus);
                booking.setCancelledAt(now);
                booking.setCancellationReason("Удаление аккаунта");
                bookingRepository.save(booking);
            }
        }

        // Инвалидируем refresh-токены
        refreshTokenRepository.revokeAllForClient(clientId, OffsetDateTime.now());
        log.info("Account deleted for client {}", clientId);
    }

    public void requestPhoneChangeCode(UUID clientId, String newPhone) {
        if (clientRepository.existsByPhoneAndDeletedAtIsNull(newPhone)) {
            throw new BusinessException("Phone already in use", HttpStatus.CONFLICT, "conflict");
        }

        String code = String.format("%04d", (int) (Math.random() * 10000));
        String hash = passwordEncoder.encode(code);
        OtpCode otp = OtpCode.builder()
                .phone(newPhone)
                .purpose(OtpPurpose.phone_change)
                .codeHash(hash)
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .resendAfter(OffsetDateTime.now().plusSeconds(60))
                .build();
        otpCodeRepository.save(otp);
        log.info("Phone change OTP for client {}: {}", clientId, code);
    }

    public ClientDto confirmPhoneChange(UUID clientId, ChangePhoneConfirmRequest request) {
        OtpCode otp = otpCodeRepository.findActiveByPhoneAndPurpose(
                request.getNewPhone(),
                OtpPurpose.phone_change,
                OffsetDateTime.now()
        ).orElseThrow(() -> new BusinessException("Неверный код или истёк срок", HttpStatus.BAD_REQUEST, "invalid_code"));

        if (!passwordEncoder.matches(request.getCode(), otp.getCodeHash())) {
            otp.setAttempts((short) (otp.getAttempts() + 1));
            otpCodeRepository.save(otp);
            throw new BusinessException("Неверный код", HttpStatus.BAD_REQUEST, "invalid_code");
        }

        otp.setConsumedAt(OffsetDateTime.now());
        otpCodeRepository.save(otp);

        Client client = findClient(clientId);
        if (clientRepository.existsByPhoneAndDeletedAtIsNull(request.getNewPhone())) {
            throw new BusinessException("Phone already in use", HttpStatus.CONFLICT, "conflict");
        }
        client.setPhone(request.getNewPhone());
        Client saved = clientRepository.save(client);
        return toClientDto(saved);
    }

    private Client findClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client not found", HttpStatus.NOT_FOUND, "not_found"));
    }

    private ClientDto toClientDto(Client client) {
        return new ClientDto(
                client.getId(),
                client.getName(),
                client.getPhone(),
                client.getCreatedAt()
        );
    }
}