# Промпты генерации реализации — картинг-центр «Апекс»

> Мастер-документ реконструирует последовательность запросов к ИИ software architect /
> tech lead, которыми была получена реализация проекта «Апекс» поверх готовой аналитики
> (`analysis/`): архитектурные документы, схема БД и Docker-инфраструктура, доменная модель
> и persistence-слой, авторизация и безопасность, бизнес-логика и REST API, SSR-фронтенд.
> Каждый раздел ниже — самостоятельный промпт в формате, аналогичном приложенному примеру
> (`implementation-plam-prompt.md` → `implementation-plan.md`), и соответствует одной группе
> реально существующих в репозитории артефактов. Разделы идут в порядке зависимостей.

**Роль во всех запросах:** Ты — опытный software architect и tech lead, специализирующийся
на Spring Boot backend-разработке и server-rendered web-приложениях.

**Сквозной контекст проекта** (передаётся во все запросы ниже):
- Клиентское веб-приложение картинг-центра «Апекс» для самостоятельной записи на заезды.
- Стек: **Java 17 + Spring Boot 3.1.5 + Spring Data JPA + Spring Security + PostgreSQL 16 +
  Flyway + JJWT 0.11.5 + Lombok + JSP/JSTL (server-rendered views) + TailwindCSS (через CDN,
  без сборки) + axios (vanilla JS, без SPA-фреймворка) + Docker/Docker Compose**.
- Базовый пакет: `ru.vsu.cs.yesikov`.
- Готовые источники истины: `analysis/1-elicitation/domain-description.md`,
  `analysis/2-requirements/*.md`, `analysis/3-design-brief/*.md`,
  `analysis/4-design/data-model.md`, `analysis/4-design/api-sequence.md`,
  `analysis/api/openapi.yaml`, `analysis/screens/*.md`, `analysis/09-logic/*.md`.
- В приложении два независимых входных слоя на одном backend: (1) **REST API** под
  `/api/**`, строго реализующий контракт `openapi.yaml` с JWT Bearer-авторизацией — им
  пользуется «настоящий» мобильный/веб-клиент; (2) **SSR-демонстрационный слой** на
  JSP-страницах с сессионной авторизацией (`HttpSession`) — упрощённый веб-интерфейс поверх
  тех же application-сервисов, для показа сценариев без отдельного SPA-фронтенда. Бизнес-
  правила не дублируются между слоями — оба обращаются к одним и тем же сервисам.
- Не добавляй функций, которых нет в `analysis/` и `openapi.yaml`. Роли Маршал/Владелец,
  админ-панель, онлайн-оплата, программа лояльности, авто-учёт погоды — вне скоупа, как
  явно зафиксировано в домене.

---

## Запрос 1 — `Архитектурный план.md`, `Схема данных.md`

**Файлы:** `Архитектурный план.md`, `Схема данных.md`

**Источники:** весь `analysis/` (в первую очередь `domain-description.md`,
`data-model.md`, `api-sequence.md`, `openapi.yaml`, `09-logic/*.md`).

**Промпт:**

На основе полной аналитики составь два верхнеуровневых архитектурных документа для команды
разработки — не план по спринтам (он будет отдельным запросом), а описание системы «как
она устроена».

**`Архитектурный план.md`:**
- Введение: цель документа, контекст проекта (кратко, в 2–3 абзаца, со ссылкой на бриф и
  домен), явное разделение «скоуп текущей работы» (клиентское приложение + API) / «вне
  скоупа» (существующая инфраструктура маршала и владельца).
- Таблица архитектурных принципов (mobile-first, разделение ответственности, атомарность
  на сервере, идемпотентность, минимальный порог входа, безопасность данных, прозрачность
  правил) — каждый принцип с колонкой «Реализация».
- Обзор системы: компоненты (клиент, бэкенд, БД), основные сценарии верхнего уровня
  (авторизация, просмотр слотов, запись, отмена, оценка, push) без деталей реализации.
- Модель данных: перечисли ключевые сущности одной строкой на каждую (без полного
  повторения `data-model.md` — только имя, назначение, ключевые поля, дай ссылку на
  `Схема данных.md` за подробностями).
- Таблица «Основные эндпоинты API» — по одному ряду на каждый путь из `openapi.yaml`.
- Таблица «Логика приложения» — по одной строке на каждую логику из `09-logic/_INDEX.md`
  с указанием, на каких экранах/эндпоинтах она применяется.
- Раздел «Инфраструктура»: БД (образ, деплой, миграции, переменные окружения, volume),
  backend (стек, транзакции, маппинг ошибок), клиент (только пометка, что клиентская часть
  реализована как SSR веб-демо поверх того же backend, без отдельного мобильного билда).
- Раздел «Безопасность»: аутентификация, сессии/токены, хранение, защита от перебора,
  защита от двойных броней, конфиденциальность данных.
- Раздел «Мониторинг и логирование» — как задел на будущее, без избыточной детализации.
- Раздел «План развития» — перечисли то, что явно вынесено вне скоупа доменом (онлайн-
  оплата, лояльность, погода, уведомления другими каналами, доработка админки).
- Заключение — 2–3 предложения.

**`Схема данных.md`:**
- ERD-диаграмма (mermaid `erDiagram`), идентичная понятийно `data-model.md`, но
  дополненная полями, которые появятся только на уровне реализации (`meeting_point`,
  `address`, `duration_minutes` у Slot) — они уже упомянуты в домене как приходящие из API,
  но в `data-model.md` их не было явно, зафиксируй здесь.
- Таблицы атрибутов по каждой сущности с колонкой «Режим» (Read-only / Write-once /
  Write-Read), синхронизированные с `openapi.yaml` дословно по именам полей.
- Диаграмма жизненного цикла брони (mermaid `flowchart`) и таблица «Из / Событие / В /
  Эффект на слот».
- Раздел «Ключевые инварианты целостности» — те же шесть инвариантов, что в
  `data-model.md`, но сформулированные как требования к реализации (то, что должно быть
  гарантировано триггерами/транзакциями БД, а не просто описанием домена).
- Раздел «Известные пробелы контракта» — явно перечисли места, где контракт API не
  покрывает 100% домена и решение принимается на уровне реализации: отсутствие статуса
  `completed` у брони (эвристика по `start_at + duration_minutes`), наличие
  `GET /ratings?booking_id=` как вспомогательного эндпоинта при основной защите через
  локальный кэш + `409`, опциональность полей `meeting_point`/`address`.
- Раздел «Соответствие ролям и границам скоупа» — какие сущности read-only для клиента
  API, какие управляются приложением.

Оба документа должны использовать одну и ту же терминологию, что и `analysis/`, — не
переименовывай сущности и статусы.

---

## Запрос 2 — Схема БД и Docker-инфраструктура

**Файлы:** `database/migrations/V1__init_schema.sql`, `database/Dockerfile`,
`docker-compose.yml`, `docker-compose.test.yml`, `dockerfile` (сборка backend),
`.env`, `pom.xml`, `src/main/resources/application.yml`

**Источники:** `Схема данных.md`, `analysis/api/openapi.yaml`, `analysis/4-design/data-model.md`.

**Промпт:**

Реализуй схему PostgreSQL и локальную инфраструктуру запуска для backend на Spring Boot,
до того как будет написана хоть одна строчка Java-кода — миграция должна стать источником
истины для JPA-сущностей, которые появятся на следующем шаге.

**`database/migrations/V1__init_schema.sql`** (PostgreSQL 15+, единственная миграция,
совместимая одновременно с двумя способами использования — как init-скрипт Docker-образа
БД и как Flyway-миграция `src/main/resources/db/V1__init_schema.sql` внутри backend,
без изменений в содержимом файла):
- расширение `pgcrypto`, вспомогательная функция/триггер `set_updated_at()`;
- ENUM-типы, дословно повторяющие enum'ы `openapi.yaml`: `track_config_type`,
  `slot_status`, `booking_status`, `push_platform`, `otp_purpose`,
  `push_notification_type`;
- таблицы `clients` (с soft delete через `deleted_at` и уникальностью телефона только среди
  «живых» записей), `track_configurations` (с сидом двух строк — Короткая/Длинная — и
  `max_group_size`), `marshals` (с денормализованным `rating_avg`/`rating_count`), `slots`
  (все поля контракта `Slot`, включая `meeting_point`/`address`/`duration_minutes`, с
  CHECK-ограничениями на диапазоны и кросс-табличным триггером проверки потолка новичковой
  конфигурации), `idempotency_keys` (композитный PK `idempotency_key + client_id +
  endpoint`, хранение фингерпринта тела запроса), `bookings` (с уникальным индексом «одна
  активная бронь клиента на слот» для защиты от двойных броней и триггерами резервирования/
  освобождения инвентаря слота при создании/отмене — резервирование обязано делать
  `SELECT ... FOR UPDATE` перед проверкой остатков, чтобы гарантировать «0 двойных броней»
  под конкурентной нагрузкой), `ratings` (уникальность `booking_id`, триггер проверки
  права на оценку — активная бронь, заезд завершён по времени, совпадение клиента и
  маршала — и триггер пересчёта `rating_avg`/`rating_count` маршала), `otp_codes`,
  `refresh_tokens`, `push_tokens`, `push_notifications_log`;
- вспомогательные `VIEW`: пересчёт доступности слота «с нуля» для сверки/аудита и
  определение «заезд завершён» как серверный аналог клиентской эвристики
  `completed_locally`;
- бизнес-исключения из триггеров подавай через `RAISE EXCEPTION USING ERRCODE = 'P000X'`
  с уникальными кодами на каждый бизнес-конфликт (отменённый слот, нет мест, нет проката,
  доступ запрещён, заезд не завершён/уже начался) — они должны быть перехватываемыми на
  уровне Java-приложения и маппиться на HTTP-коды `openapi.yaml`;
- в конце файла — комментарий-инструкция для backend-разработчика: как маппить эти
  ERRCODE на HTTP-статусы и бизнес-коды ошибок контракта.

**`database/Dockerfile`** — на основе `postgres:16-alpine`, копирует все `*.sql` из
`migrations/` в `/docker-entrypoint-initdb.d/`, здоровье через `pg_isready`.

**`docker-compose.yml`** (корень репозитория) — три сервиса: `db` (собирается из
`database/Dockerfile`, переменные из `.env`, volume для персистентности, healthcheck),
`backend` (собирается из корневого `dockerfile`, зависит от `db` по healthcheck, полный
набор переменных окружения для подключения к БД и JWT-секрета), `adminer` (опционально,
через Docker Compose `profiles: [tools]`, только для локальной отладки БД). Именованные
volume и сеть — с префиксом проекта.

**Корневой `dockerfile`** — многоэтапная сборка: `maven:3.9.6-eclipse-temurin-17-alpine`
для сборки jar (`mvn dependency:go-offline` отдельным слоем для кэша), затем
`eclipse-temurin:17-jre-alpine` для запуска.

**`.env`** — переменные для Postgres (`POSTGRES_USER/PASSWORD/DB/PORT`), backend
(`BACKEND_PORT`, `JWT_SECRET` не короче 32 символов), adminer (`ADMINER_PORT`).

**`docker-compose.test.yml`** — сервис `test-runner` на `python:3.11-slim` для
интеграционных тестов API, подключается к общей внешней сети с backend/db, ставит
зависимости из `tests/integration/requirements.txt` и запускает `pytest`.

**`pom.xml`** — Spring Boot 3.1.5 parent, Java 17: `spring-boot-starter-web`,
`spring-boot-starter-data-jpa`, `spring-boot-starter-security`,
`spring-boot-starter-validation`, `spring-boot-starter-actuator`, `postgresql` (runtime),
`h2` (runtime, для dev-профиля), `flyway-core`, `jjwt-api/impl/jackson` 0.11.5,
`springdoc-openapi-starter-webmvc-ui`, `lombok`, `tomcat-embed-jasper` + `jstl` (для JSP-
представлений), тестовые зависимости (`spring-boot-starter-test`,
`spring-security-test`, `testcontainers` + `testcontainers-postgresql`).

**`src/main/resources/application.yml`** — три секции через `---`: базовая (Flyway
locations, `ddl-auto: validate`, JWT-параметры из переменных окружения), профиль `dev`
(H2 в памяти, `ddl-auto: create-drop`, Flyway и `@Scheduled`-джобы выключены, чтобы не
требовать реальных enum-кастов Postgres), профиль `default` (PostgreSQL по переменным
окружения `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`, `ddl-auto: validate`, Flyway и
безопасность включены).

---

## Запрос 3 — Доменная модель и persistence-слой

**Файлы:** `src/main/java/ru/vsu/cs/yesikov/model/*.java` (и `model/enums/*.java`),
`src/main/java/ru/vsu/cs/yesikov/repository/*.java`, `KartingApplication.java`

**Источники:** `database/migrations/V1__init_schema.sql`, `Схема данных.md`.

**Промпт:**

Реализуй JPA-сущности и Spring Data репозитории один в один по уже готовой схеме БД —
не меняй имена таблиц/колонок и ограничения, только маппи их на Java.

- Точка входа `KartingApplication.java` с `@SpringBootApplication` и `@EnableScheduling`
  (нужен для push-джоб на будущем шаге).
- Enum'ы `model/enums/`: `TrackConfigType`, `SlotStatus`, `BookingStatus`, `PushPlatform`,
  `OtpPurpose`, `PushNotificationType` — простые Java enum, один в один с Postgres ENUM.
- Сущности `model/`: `Client` (soft delete через `deletedAt`, `@PrePersist`/`@PreUpdate`
  для `createdAt`/`updatedAt`), `TrackConfiguration`, `Marshal` (денормализованные
  `ratingAvg`/`ratingCount`), `Slot` (все поля контракта; `freeKarts`/`freeRentalGear`
  помечены `insertable = false, updatable = false` — они управляются только триггерами
  БД, Java-код их не пишет напрямую), `Booking` (связи `@ManyToOne` на `Slot`/`Client`,
  `idempotencyKey`, `cancelledAt`), `Rating` (`@OneToOne` на `Booking` с `unique = true`),
  `PushToken`, `PushNotificationLog`, `OtpCode`, `RefreshToken` (с self-reference
  `replacedBy` для цепочки ротации), `IdempotencyKey` с составным `IdempotencyKeyId`
  (`@IdClass`) на `(idempotencyKey, clientId, endpoint)`.
- Используй Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) на
  всех сущностях, `@GeneratedValue(strategy = GenerationType.UUID)` для всех PK.
- Репозитории `repository/`: по одному `JpaRepository<Entity, UUID>` на сущность, где
  нужно — с `@Query`. Обязательно включи в `BookingRepository`:
  `existsActiveBookingForClientAndSlot` (для явной проверки `double_booking` до похода в
  БД), `findByIdempotencyKeyAndClientId` (для идемпотентного повтора запроса),
  JOIN FETCH-варианты `findByIdWithSlot`/`findAllByClientIdWithSlot` (устранение N+1 при
  отдаче вложенного `Slot`/`TrackConfiguration`/`Marshal` в ответах API), нативные запросы
  для двух push-джоб (брони по отменённым слотам без отправленного уведомления; брони,
  стартующие в ближайшие 2 часа без отправленного напоминания). В `RatingRepository` —
  `findByBookingId`/`existsByBookingId`. В `RefreshTokenRepository` — поиск активных
  токенов клиента и групповая ревокация при логауте. `SlotRepository` должен
  реализовывать `JpaSpecificationExecutor` для динамической фильтрации по дате/трассе/
  маршалу/наличию мест.

Не добавляй сервисного/контроллерного кода на этом шаге — только модель данных и доступ
к ней.

---

## Запрос 4 — Аутентификация и безопасность

**Файлы:** `security/jwt/JwtUtils.java`, `security/jwt/JwtProperties.java`,
`security/jwt/JwtAuthenticationFilter.java`, `config/WebSecurityConfig.java`,
`config/PasswordEncoderConfig.java`, `service/AuthService.java`,
`dto/auth/*.java`, `controller/AuthController.java`

**Источники:** `analysis/09-logic/LOGIC-001-auth-otp.md`, `analysis/api/openapi.yaml`
(теги Auth), доменная модель из Запроса 3.

**Промпт:**

Реализуй вход по телефону + одноразовый SMS-код и управление JWT-сессией строго по
`LOGIC-001-auth-otp.md`, без пароля где-либо в системе.

- `JwtProperties` — `@ConfigurationProperties(prefix = "jwt")`: `secret`,
  `accessTokenTtl`, `refreshTokenTtl` (секунды).
- `JwtUtils` — генерация access/refresh токенов (HS256, subject = `clientId`, claim
  `phone` в access-токене), извлечение claims, проверка истечения и валидности.
- `JwtAuthenticationFilter` (`OncePerRequestFilter`, без `@Component` — регистрируется
  вручную в security-конфиге) — читает `Authorization: Bearer`, при валидном токене
  кладёт `clientId` в `SecurityContextHolder` как principal.
- `WebSecurityConfig` — `/api/auth/**` открыт без токена (`security: []` в контракте),
  все остальные `/api/**` требуют аутентификации, JSP/статические ресурсы и `/h2-console`
  открыты без ограничений (SSR-слой использует собственную ручную сессионную проверку в
  контроллерах, а не Spring Security `formLogin` — его нужно явно отключить), CSRF
  отключить (stateless API + сессионный SSR-слой не использует формы с CSRF-защитой в
  данном MVP).
- `PasswordEncoderConfig` — bean `BCryptPasswordEncoder`, используется для хэширования
  OTP-кода и refresh-токена (никогда не хранить их в открытом виде).
- `AuthService.requestCode` — генерирует 4-значный код, хэширует, сохраняет
  `OtpCode` с `expiresAt`/`resendAfter`; `verifyCode` — ищет активный код по телефону и
  назначению `login`, сверяет через `passwordEncoder.matches`, при неверном коде считает
  попытки и бросает `invalid_code` (400); при успехе — находит или создаёт `Client` (для
  нового — пустое имя, `is_new = true`), выпускает пару токенов, сохраняет хэш refresh-
  токена. Отдельно предусмотри тестовый обходной путь только под Spring-профилем `test`
  (фиксированный код `1234`), чтобы integration-тесты не зависели от реальной отправки SMS.
- `AuthService.refreshToken` — по subject из JWT ищет все активные (не отозванные, не
  истёкшие) refresh-токены клиента, сверяет через `matches` (так как хранится только
  хэш), при совпадении делает ротацию (старый — revoked, новый — сохранён), при
  отсутствии совпадения — `401 unauthorized`.
- `AuthService.logout` — массовая ревокация всех активных refresh-токенов клиента.
- `dto/auth/` — DTO один в один со схемами `openapi.yaml`: `RequestCodeRequest/Response`,
  `VerifyCodeRequest/Response`, `TokenPairResponse`, `RefreshTokenRequest`,
  `PushTokenRequest`/`PushTokenDeleteRequest` — с `jakarta.validation`-аннотациями,
  соответствующими форматам контракта (E.164 для телефона, 4–6 цифр для кода).
- `AuthController` — эндпоинты `/api/auth/request-code`, `/verify-code`, `/refresh`,
  `/logout`, `/push-tokens` (POST/DELETE), с `@AuthenticationPrincipal UUID clientId` там,
  где нужен авторизованный контекст.

---

## Запрос 5 — Бизнес-логика и REST API

**Файлы:** `dto/**` (кроме `auth/`), `service/ProfileService.java`,
`service/SlotService.java`, `service/BookingService.java`, `service/RatingService.java`,
`service/PushService.java`, `controller/ProfileController.java`,
`controller/SlotController.java`, `controller/BookingController.java`,
`controller/InstructorController.java`, `controller/RatingController.java`,
`exception/BusinessException.java`, `exception/SqlErrorMapper.java`,
`exception/GlobalExceptionHandler.java`

**Источники:** `analysis/api/openapi.yaml` (все теги кроме Auth),
`analysis/09-logic/LOGIC-002..006-*.md`, `analysis/4-design/api-sequence.md`.

**Промпт:**

Реализуй прикладную бизнес-логику и REST-контроллеры для всех оставшихся тегов
`openapi.yaml`, каждый эндпоинт должен возвращать в точности те HTTP-коды и машинные
коды ошибок, что описаны в контракте и в `api-sequence.md`.

- **Profile.** `ProfileService`: получение/обновление имени, удаление аккаунта (soft
  delete + каскадная отмена всех активных броней клиента с причиной «Удаление аккаунта» +
  ревокация refresh-токенов), смена телефона в два шага (запрос кода на новый номер с
  проверкой уникальности → подтверждение кодом). `ProfileController` — соответствующие
  `GET/PATCH/DELETE /api/profile`, `POST /api/profile/phone/request-code`,
  `POST /api/profile/phone/confirm`.
- **Slots.** `SlotService.listSlots` — `Specification`-based фильтрация: дефолт `dateFrom
  = now`, `dateTo = dateFrom + 7 дней` (R-027), фильтры по `track_config`/
  `instructor_id`/`only_available` (строгое `free_karts > 0`), всегда только
  `status = scheduled`. `getSlot` — полная карточка по id. Вынеси `toSummary`/
  `toFullResponse` как переиспользуемые мапперы Slot → DTO (используются также из
  `BookingService`). `SlotController` — `GET /api/slots`, `GET /api/slots/{slotId}`.
- **Instructors.** `InstructorController` — простой read-only `GET /api/instructors` с
  пагинацией, без отдельного сервиса.
- **Bookings.** `BookingService.createBooking` реализует ровно LOGIC-003:
  1) идемпотентность — при повторном запросе с тем же `Idempotency-Key` и тем же
  фингерпринтом тела вернуть уже созданную бронь вместо ошибки; при том же ключе, но
  другом теле — `409 idempotency_key_conflict`;
  2) проверка отменённого слота (`410 slot_cancelled`);
  3) проверка, что заезд ещё не начался (`422 slot_started`);
  4) проверка отсутствия уже активной брони клиента на слот (`409 double_booking`);
  5) предварительная advisory-проверка лимитов мест/проката по данным слота (`409
  slot_full`/`409 gear_unavailable` с деталями остатка) — до похода в БД, для понятного
  сообщения без лишней транзакции;
  6) сохранение брони; если между проверкой и вставкой данные устарели (конкурентная
  гонка) — триггер БД бросит ERRCODE, поймай `DataAccessException` и перегони её через
  `SqlErrorMapper` в тот же контрактный код ошибки, что и явные проверки выше.
  `cancelBooking` реализует LOGIC-004: проверка владения (`403`), проверка что бронь ещё
  активна (`409 already_cancelled`), проверка что заезд не начался (`422 slot_started`),
  определение ранней/поздней отмены сравнением `now + 1 час` с `slot.startAt` (граница
  ровно в час — ранняя отмена). `listBookings`/`getBooking` — с пагинацией и разделением
  по статусам, JOIN FETCH везде, где отдаётся вложенный `Slot`.
  `BookingController` — `POST /api/bookings` (с обязательным заголовком
  `Idempotency-Key`), `GET /api/bookings`, `GET /api/bookings/{bookingId}`,
  `POST /api/bookings/{bookingId}/cancel`.
- **Ratings.** `RatingService.createRating` реализует LOGIC-005: проверка владения
  бронью (`403`), проверка что бронь активна (`403 forbidden` — нельзя оценить отменённую
  бронь), проверка что заезд завершён по `slot.startAt + slot.durationMinutes` (`422
  unprocessable`, если рано), проверка совпадения маршала с броней (`400 bad_request`),
  проверка что оценка ещё не оставлена (`409 conflict`) — и лишь после всех проверок
  сохранение оценки (пересчёт `rating_avg` маршала выполняет триггер БД, из Java явно
  ничего не пересчитывай). `getRatingByBooking` — простой поиск по `booking_id`.
  `RatingController` — `POST /api/ratings`, `GET /api/ratings?booking_id=`.
- **Push (внутренняя часть).** `PushService` — регистрация/удаление push-токена
  (идемпотентно — удалить старую запись с тем же `(token, platform)` перед вставкой новой),
  отправка уведомления об отмене центром с логированием и записью в
  `push_notifications_log` для защиты от повторной отправки, две `@Scheduled`-джобы
  (обработка отменённых центром слотов раз в минуту, напоминания за 2 часа до старта раз
  в 5 минут) — обе должны быть no-op в dev-профиле, чтобы не требовать реальных Postgres
  enum-кастов на H2. Реальная интеграция с APNs/FCM — вне скоупа MVP, здесь только
  журналирование и идемпотентная джоба.
- **Обработка ошибок.** `BusinessException` — RuntimeException с HTTP-статусом, машинным
  кодом и опциональными `details` (для `free_karts`/`free_rental_gear` в конфликтах).
  `SqlErrorMapper` — единая точка маппинга SQLSTATE триггеров (`P0001`…`P0006`) и
  `unique_violation` (`23505`, по имени constraint) в `BusinessException` с нужным
  HTTP-статусом и кодом. `GlobalExceptionHandler` — `@ControllerAdvice` с обработчиками
  `BusinessException`, `MethodArgumentNotValidException` (→ `400 bad_request`),
  `DataAccessException` (основной путь для ошибок триггеров БД — Spring Data оборачивает
  исходный `SQLException`, поэтому именно этот обработчик, а не голый
  `SQLException`-хендлер, ловит большинство бизнес-конфликтов от гонок), и generic
  `Exception` (→ `500 internal_error`, без утечки стектрейса в тело ответа).

Все Response DTO должны один в один соответствовать схемам `openapi.yaml` (включая
`PaginationMeta`, `Error` с закрытым перечислением кодов).

---

## Запрос 6 — SSR веб-фронтенд (JSP)

**Файлы:** `web/WebController.java`, `web/dto/LoginForm.java`, `web/dto/BookingForm.java`,
`config/WebMvcConfig.java`, `src/main/webapp/WEB-INF/jsp/*.jsp`

**Источники:** `analysis/screens/*.md`, `analysis/3-design-brief/00-foundations.md`,
application-сервисы из Запросов 4–5.

**Промпт:**

Собери упрощённый server-rendered веб-интерфейс поверх уже готовых application-сервисов
(`AuthService`, `SlotService`, `BookingService`, `RatingService`) — это демонстрационный
слой поверх тех же бизнес-правил, не отдельная реализация логики. Экраны из
`analysis/screens/` бери за основу состава полей и порядка действий, но верстай как
классические JSP-страницы с постбэками форм, а не как мобильный UI с bottom sheet —
критичные подтверждения (отмена, оценка) реализуй через простые модальные `<div>` с
JS-показом/скрытием и вызовом соответствующего эндпоинта через `axios`.

- `WebMvcConfig` — JSP view resolver (`/WEB-INF/jsp/`, суффикс `.jsp`, `JstlView`),
  раздача статики.
- `web/dto/LoginForm.java` — `phone` с той же E.164-валидацией, что и в API DTO.
- `web/dto/BookingForm.java` — `seatsCount`/`rentalGearCount` с `@Min`.
- `WebController`:
  - `GET/POST /login`, `POST /request-code` (SCR-001 шаг 1), `POST /verify-code`
    (SCR-001 шаг 2, сохраняет `clientId`/`client`/`isNew` в `HttpSession`),
    `POST /logout`;
  - `GET /slots` (SCR-002: фильтры как query-параметры формы, редирект на `/login` при
    отсутствии сессии), `GET /slots/{slotId}` (SCR-003);
  - `GET/POST /booking/{slotId}` (SCR-004: степпер мест и проката через vanilla JS,
    итоговая цена пересчитывается на клиенте только справочно, финальная — из ответа
    сервера), `GET /booking-success/{bookingId}` (BS-002 в упрощённом виде — отдельная
    страница вместо шторки);
  - `GET /bookings` (SCR-005), `GET /bookings/{bookingId}` (SCR-006 с блоками отмены/
    оценки/статуса «Отменён центром»);
  - AJAX-эндпоинты для критичных действий без полной перезагрузки страницы:
    `POST /web/bookings/{bookingId}/cancel` (BS-003 — подтверждение через JS-модалку,
    вызывает `BookingService.cancelBooking`), `POST /web/ratings` (BS-004 — вызывает
    `RatingService.createRating`); используй отдельный префикс `/web/` для этих двух
    action-эндпоинтов, чтобы не путать их с публичным REST API под `/api/`.
  - Каждый обработчик, кроме `/login`/`/request-code`/`/verify-code`, должен проверять
    наличие `clientId` в сессии и редиректить на `/login` при его отсутствии.
- JSP-страницы (`login.jsp`, `login-code.jsp`, `slot-list.jsp`, `slot-card.jsp`,
  `booking.jsp`, `booking-success.jsp`, `my-bookings.jsp`, `booking-details.jsp`):
  - TailwindCSS через `<script src="https://cdn.tailwindcss.com">` — без сборки, для MVP
    этого достаточно;
  - JSTL (`c:choose`/`c:forEach`/`c:if`, `fmt:formatDate`/`fmt:formatNumber`) для условной
    отрисовки состояний Loading/Content/Empty (пустые списки — текст-заглушка, как в
    `00-foundations.md` §5, без отдельного визуального skeleton-состояния — это упрощение
    для SSR-версии);
  - статус-бейджи брони (`active`/`cancelled`/`late_cancel`/`center_cancelled`) — с
    подписями и цветовым кодированием, дословно как в таблице статусов
    `SCR-005-my-bookings.md`;
  - на `booking-details.jsp` — модалка отмены (текст правила «1 час», без штрафов) и
    модалка оценки (5 звёзд кликабельно через vanilla JS + textarea для комментария до
    500 символов), обе вызывают соответствующий `/web/...` эндпоинт через `axios` и
    перезагружают страницу при успехе.

Не переноси в JSP ничего, что должно валидироваться только на сервере повторно — форма
может дублировать простую клиентскую проверку (например, disabled-кнопку при 0 выбранной
оценке), но окончательное решение всегда за ответом backend.
