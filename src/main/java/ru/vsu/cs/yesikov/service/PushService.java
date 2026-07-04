package ru.vsu.cs.yesikov.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.model.Booking;
import ru.vsu.cs.yesikov.model.Client;
import ru.vsu.cs.yesikov.model.PushNotificationLog;
import ru.vsu.cs.yesikov.model.PushToken;
import ru.vsu.cs.yesikov.model.enums.BookingStatus;
import ru.vsu.cs.yesikov.model.enums.PushNotificationType;
import ru.vsu.cs.yesikov.model.enums.PushPlatform;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;
import ru.vsu.cs.yesikov.repository.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PushService {

    private final PushTokenRepository pushTokenRepository;
    private final ClientRepository clientRepository;
    private final PushNotificationLogRepository pushLogRepository;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final Environment environment;

    // --- Регистрация / удаление токенов ---

    public void registerPushToken(UUID clientId, String token, PushPlatform platform) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        pushTokenRepository.deleteByClientIdAndTokenAndPlatform(clientId, token, platform);
        PushToken pt = PushToken.builder()
                .client(client)
                .token(token)
                .platform(platform)
                .build();
        pushTokenRepository.save(pt);
        log.info("Push token registered for client {}", clientId);
    }

    public void deletePushToken(UUID clientId, String token, PushPlatform platform) {
        pushTokenRepository.deleteByClientIdAndTokenAndPlatform(clientId, token, platform);
        log.info("Push token deleted for client {}", clientId);
    }

    // --- Отправка уведомления об отмене центром ---

    public void notifyCenterCancellation(UUID bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        List<PushToken> tokens = pushTokenRepository.findAllByClientId(booking.getClient().getId());
        for (PushToken token : tokens) {
            log.info("Sending center cancellation push to client {}: booking {} cancelled. Reason: {}",
                    booking.getClient().getId(), bookingId, reason);
        }
        PushNotificationLog logEntry = PushNotificationLog.builder()
                .client(booking.getClient())
                .booking(booking)
                .type(PushNotificationType.center_cancelled)
                .build();
        pushLogRepository.save(logEntry);
    }

    // --- Джоба: обработка отмен центром ---

    @Scheduled(fixedDelay = 60000)
    public void processCenterCancellations() {
        // В профиле dev ничего не делаем (избегаем ошибок CAST)
        if (environment.acceptsProfiles("dev")) {
            log.debug("PushService is disabled in dev profile, skipping center cancellations");
            return;
        }

        log.debug("Checking for center-cancelled slots...");
        List<Booking> activeBookingsForCancelledSlots = bookingRepository
                .findActiveBySlotStatusAndNotNotified(
                        SlotStatus.cancelled.name(),
                        PushNotificationType.center_cancelled.name()
                );
        for (Booking booking : activeBookingsForCancelledSlots) {
            String reason = booking.getSlot().getCancellationReason();
            if (reason == null) reason = "Заезд отменён администрацией";
            notifyCenterCancellation(booking.getId(), reason);
            booking.setStatus(BookingStatus.center_cancelled);
            booking.setCancelledAt(OffsetDateTime.now());
            booking.setCancellationReason(reason);
            bookingRepository.save(booking);
        }
        if (!activeBookingsForCancelledSlots.isEmpty()) {
            log.info("Processed {} center cancellations", activeBookingsForCancelledSlots.size());
        }
    }

    // --- Джоба: напоминания перед заездом ---

    @Scheduled(cron = "0 */5 * * * *")
    public void sendReminders() {
        // В профиле dev ничего не делаем (избегаем ошибок CAST)
        if (environment.acceptsProfiles("dev")) {
            log.debug("PushService is disabled in dev profile, skipping reminders");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime twoHoursLater = now.plusHours(2);
        List<Booking> bookingsForReminder = bookingRepository
                .findActiveByStartTimeBetweenAndNotNotified(
                        now,
                        twoHoursLater,
                        PushNotificationType.reminder.name()
                );
        for (Booking booking : bookingsForReminder) {
            List<PushToken> tokens = pushTokenRepository.findAllByClientId(booking.getClient().getId());
            for (PushToken token : tokens) {
                log.info("Sending reminder push to client {}: booking {} starts at {}",
                        booking.getClient().getId(), booking.getId(), booking.getSlot().getStartAt());
            }
            PushNotificationLog logEntry = PushNotificationLog.builder()
                    .client(booking.getClient())
                    .booking(booking)
                    .type(PushNotificationType.reminder)
                    .build();
            pushLogRepository.save(logEntry);
        }
        if (!bookingsForReminder.isEmpty()) {
            log.info("Sent {} reminders", bookingsForReminder.size());
        }
    }
}