package ru.vsu.cs.yesikov.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.vsu.cs.yesikov.model.Marshal;
import ru.vsu.cs.yesikov.model.Slot;
import ru.vsu.cs.yesikov.model.TrackConfiguration;
import ru.vsu.cs.yesikov.model.enums.SlotStatus;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;
import ru.vsu.cs.yesikov.repository.MarshalRepository;
import ru.vsu.cs.yesikov.repository.SlotRepository;
import ru.vsu.cs.yesikov.repository.TrackConfigurationRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MarshalRepository marshalRepository;
    private final SlotRepository slotRepository;
    private final TrackConfigurationRepository trackConfigurationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Проверяем, есть ли уже маршалы (если есть, значит данные уже загружены)
        if (marshalRepository.count() > 0) {
            System.out.println("ℹ️ Данные уже загружены, пропускаем инициализацию.");
            return;
        }

        // 1. Создаём конфигурации трасс, если их нет
        TrackConfiguration novice = trackConfigurationRepository.findByType(TrackConfigType.novice)
                .orElseGet(() -> {
                    System.out.println("Создаём конфигурацию 'Короткая'...");
                    TrackConfiguration tc = TrackConfiguration.builder()
                            .name("Короткая")
                            .type(TrackConfigType.novice)
                            .description("Новичковая трасса, часть картов с ограничением скорости")
                            .maxGroupSize((short) 8)
                            .build();
                    return trackConfigurationRepository.save(tc);
                });

        TrackConfiguration experienced = trackConfigurationRepository.findByType(TrackConfigType.experienced)
                .orElseGet(() -> {
                    System.out.println("Создаём конфигурацию 'Длинная'...");
                    TrackConfiguration tc = TrackConfiguration.builder()
                            .name("Длинная")
                            .type(TrackConfigType.experienced)
                            .description("Трасса для опытных райдеров")
                            .maxGroupSize((short) 14)
                            .build();
                    return trackConfigurationRepository.save(tc);
                });

        // 2. Создаём маршалов
        Marshal ivan = Marshal.builder()
                .name("Иван Петров")
                .isActive(true)
                .ratingAvg(BigDecimal.valueOf(4.7))
                .ratingCount(42)
                .build();
        Marshal olga = Marshal.builder()
                .name("Ольга Смирнова")
                .isActive(true)
                .ratingAvg(BigDecimal.valueOf(4.9))
                .ratingCount(38)
                .build();
        Marshal sergey = Marshal.builder()
                .name("Сергей Иванов")
                .isActive(true)
                .ratingAvg(BigDecimal.valueOf(4.2))
                .ratingCount(25)
                .build();
        Marshal anna = Marshal.builder()
                .name("Анна Кузнецова")
                .isActive(true)
                .ratingAvg(BigDecimal.valueOf(3.8))
                .ratingCount(15)
                .build();
        List<Marshal> marshals = marshalRepository.saveAll(List.of(ivan, olga, sergey, anna));
        System.out.println("✅ Создано " + marshals.size() + " маршалов.");

        // 3. Создаём слоты на ближайшие дни
        OffsetDateTime now = OffsetDateTime.now();
        int slotsCreated = 0;

        for (int i = 0; i < 5; i++) {
            OffsetDateTime start = now.plusDays(i / 2).withHour(10 + i * 2).withMinute(0).withSecond(0).withNano(0);
            if (start.isBefore(now)) {
                start = start.plusDays(1);
            }

            Slot slot;
            if (i < 3) {
                // короткая трасса, 8 картов
                slot = Slot.builder()
                        .trackConfig(novice)
                        .marshal(i % 2 == 0 ? ivan : olga)
                        .startAt(start)
                        .durationMinutes((short) 20)
                        .totalKarts((short) 8)
                        .freeKarts((short) 8)          // <-- явно указываем
                        .totalRentalGear((short) 10)
                        .freeRentalGear((short) 10)    // <-- явно указываем
                        .priceKart(2500)
                        .priceGearRental(500)
                        .requirementsText("Возраст от 12 лет, рост от 140 см")
                        .meetingPoint("Площадка у павильона №2")
                        .address("ул. Картинговая, д. 1")
                        .status(SlotStatus.scheduled)
                        .build();
            } else {
                // длинная трасса, 14 картов
                slot = Slot.builder()
                        .trackConfig(experienced)
                        .marshal(i % 2 == 0 ? sergey : anna)
                        .startAt(start)
                        .durationMinutes((short) 20)
                        .totalKarts((short) 14)
                        .freeKarts((short) 14)         // <-- явно указываем
                        .totalRentalGear((short) 15)
                        .freeRentalGear((short) 15)    // <-- явно указываем
                        .priceKart(3500)
                        .priceGearRental(500)
                        .requirementsText("Возраст от 16 лет, рост от 150 см")
                        .meetingPoint("Площадка у павильона №2")
                        .address("ул. Картинговая, д. 1")
                        .status(SlotStatus.scheduled)
                        .build();
            }
            slotRepository.save(slot);
            slotsCreated++;
        }

        System.out.println("✅ Создано " + slotsCreated + " слотов.");
        System.out.println("🎉 Тестовые данные успешно загружены!");
    }
}