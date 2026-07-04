# 09. Логики — индекс

> Переиспользуемая бизнес- и UI-логика клиентского приложения «Апекс». Выносится один раз и
> подключается на экранах через секцию «Применяемые логики» по ссылке (принцип DRY).
> Шаблон — [`_LOGIC_TEMPLATE.md`](../_LOGIC_TEMPLATE.md).

**Статус:** Актуален · **Дата:** 2026-07-04

---

## Реестр логик

| ID | Логика | Приоритет | Назначение | Применяется на |
|----|--------|-----------|------------|----------------|
| **LOGIC-001** | [OTP-авторизация и сессия](LOGIC-001-auth-otp.md) | Critical | Вход по телефону без пароля (запрос кода, проверка, управление JWT, refresh, выход) | [SCR-001](../screens/SCR-001-registration.md) |
| **LOGIC-002** | [Расчёт доступности мест и проката](LOGIC-002-availability-calc.md) | Critical | `max_seats = min(free_karts, track_config_cap)` и `max_rental = min(free_rental_gear, seats_count)`; разделение мест и прокатного фонда | [SCR-003](../screens/SCR-003-slot-card.md), [SCR-004](../screens/SCR-004-booking.md) |
| **LOGIC-003** | [Создание брони](LOGIC-003-create-booking.md) | Critical | Отправка `POST /bookings` с Idempotency-Key, обработка всех конфликтов (мест, прокат, двойная бронь, отмена центром) | [SCR-004](../screens/SCR-004-booking.md) |
| **LOGIC-004** | [Отмена брони](LOGIC-004-cancel-booking.md) | Critical | Отмена активной брони, определение ранней/поздней отмены на сервере, локальное предупреждение по правилу 1 часа | [SCR-006](../screens/SCR-006-booking-details.md), [BS-003](../screens/BS-003-cancel-confirm.md) |
| **LOGIC-005** | [Оценка маршала](LOGIC-005-marshal-rating.md) | Medium | Определение завершённости заезда (компромиссное, т.к. нет статуса `completed` и длительности в API) и однократная необязательная оценка | [SCR-005](../screens/SCR-005-my-bookings.md), [SCR-006](../screens/SCR-006-booking-details.md), [BS-004](../screens/BS-004-marshal-rating.md) |
| **LOGIC-006** | [Push-уведомления](LOGIC-006-push-notifications.md) | High | Запрос разрешения после первой записи, регистрация/отвязка токена, обработка напоминаний и отмен центром | [BS-002](../screens/BS-002-booking-success.md) (запрос разрешения), все экраны (входящие push) |

---

## Карта «экран → логики»

| Экран | Логики |
|-------|--------|
| [SCR-001 Регистрация / Вход](../screens/SCR-001-registration.md) | LOGIC-001 |
| [SCR-002 Список слотов](../screens/SCR-002-slot-list.md) | (использует данные API, но не переиспользуемые логики) |
| [BS-001 Фильтры](../screens/BS-001-filters.md) | (собственная логика фильтрации, не вынесена) |
| [SCR-003 Карточка слота](../screens/SCR-003-slot-card.md) | LOGIC-002 |
| [SCR-004 Оформление записи](../screens/SCR-004-booking.md) | LOGIC-002, LOGIC-003 |
| [BS-002 Подтверждение записи](../screens/BS-002-booking-success.md) | LOGIC-006 (запрос push-разрешения) |
| [SCR-005 Мои бронирования](../screens/SCR-005-my-bookings.md) | LOGIC-005 (для вычисления статуса «Завершена») |
| [SCR-006 Детали брони + отмена / оценка](../screens/SCR-006-booking-details.md) | LOGIC-004, LOGIC-005 |
| [BS-003 Подтверждение отмены](../screens/BS-003-cancel-confirm.md) | LOGIC-004 |
| [BS-004 Оценка маршала](../screens/BS-004-marshal-rating.md) | LOGIC-005 |