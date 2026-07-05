# SummerSchool_2026 — «Апекс» (картинг-центр)

Учебный проект: полный цикл разработки backend-части и web-прототипа клиентского приложения для картинг-центра «Апекс» — от анализа предметной области и требований до Spring Boot API, JSP-интерфейса, SQL-схемы и тестовой документации.

Приложение решает проблему ручной записи на заезды (Telegram + маркерная доска): клиенты сами видят свободные слоты, бронируют место, управляют своими бронями, отменяют запись и оценивают маршалов после заезда.

Этот README — навигационная карта по репозиторию: что где лежит и зачем.

## Структура репозитория

```text
SummerSchool_2026/
├── analysis/                 # Аналитика: бриф, требования, дизайн, логики и OpenAPI-контракт
├── src/main/java/            # Backend на Java + Spring Boot: контроллеры, сервисы, DTO, модели, репозитории
├── src/main/resources/       # Конфигурация приложения и Flyway-миграции
├── src/main/webapp/          # JSP-страницы web-прототипа
├── database/                 # Docker-образ БД и PostgreSQL-миграции
├── tests/                    # Баг-репорты, тест-кейсы и промпты по тестированию
├── docker-compose.yml        # Запуск PostgreSQL, backend и Adminer
├── docker-compose.test.yml   # Тестовое окружение
├── dockerfile                # Dockerfile backend-приложения
├── pom.xml                   # Maven-конфигурация проекта
└── README.md                 # Навигация по репозиторию
```

## analysis/ — аналитика и дизайн

Последовательные этапы анализа, от брифа заказчика до готового API-контракта и логик приложения.

| Папка / файл | Содержимое |
| --- | --- |
| `0-customer-brief/` | Исходный бриф заказчика: `brief-karting.md`. |
| `1-elicitation/` | Сбор требований: `domain-description.md` (описание предметной области), `customer-questions.md` (вопросы заказчику). |
| `2-requirements/` | Формальные требования: бизнес-требования, функциональные и нефункциональные требования, use-case'ы и пользовательские истории. |
| `3-design-brief/` | UX/UI-бриф: `00-foundations.md` (принципы интерфейса), `design-brief.md` (карта экранов и сценариев). |
| `4-design/` | Технический дизайн: `data-model.md` (модель данных), `api-sequence.md` (последовательности API-запросов). |
| `09-logic/` | Сквозные логики: OTP-авторизация, расчёт доступности, создание брони, отмена брони, оценка маршала, push-уведомления. |
| `screens/` | Спецификации экранов и bottom sheets: регистрация, список слотов, карточка слота, бронирование, мои брони, детали брони, фильтры, подтверждения и оценка маршала. |
| `api/openapi.yaml` | OpenAPI-контракт клиентского API. |
| `analysis-prompts.md` | Лог промптов, использованных при подготовке аналитики. |

Если нужно быстро понять, что делает приложение → начинайте с `analysis/1-elicitation/domain-description.md`, затем переходите к `analysis/2-requirements/` и `analysis/api/openapi.yaml`.

## src/main/java/ — backend-приложение

Backend реализован на Java 17 + Spring Boot. В проекте есть REST API для мобильного/клиентского приложения и JSP web-прототип для ручной проверки основных пользовательских сценариев.

### Основные модули

```text
src/main/java/
├── ru/vsu/cs/yesikov/
│   ├── KartingApplication.java        # Точка входа Spring Boot
│   ├── config/                        # Конфигурация безопасности, MVC, кодировщика и начальных данных
│   ├── controller/                    # REST-контроллеры клиентского API
│   ├── dto/                           # DTO запросов и ответов API
│   ├── exception/                     # Бизнес-исключения, обработчик ошибок, маппинг SQL-ошибок
│   ├── model/                         # JPA-сущности и enum'ы доменной модели
│   ├── repository/                    # Spring Data JPA репозитории
│   ├── security/jwt/                  # JWT-фильтр, свойства и утилиты токенов
│   ├── service/                       # Бизнес-логика авторизации, слотов, броней, профиля, оценок и push-токенов
│   └── web/                           # MVC-контроллер и формы JSP web-прототипа
├── Архитектурный план.md              # Архитектурные заметки по реализации
└── Схема данных.md                    # Описание схемы данных
```

### REST API

| Контроллер | Базовый путь | Назначение |
| --- | --- | --- |
| `AuthController` | `/api/auth` | OTP-авторизация, проверка кода, refresh/logout, регистрация и удаление push-токенов. |
| `SlotController` | `/api/slots` | Получение списка слотов и детальной карточки слота. |
| `BookingController` | `/api/bookings` | Создание брони, список моих броней, детали брони, отмена брони. |
| `InstructorController` | `/api/instructors` | Список маршалов / инструкторов. |
| `ProfileController` | `/api/profile` | Просмотр, обновление и удаление профиля, смена телефона через OTP. |
| `RatingController` | `/api/ratings` | Создание оценки маршала и получение списка оценок. |

### Web-прототип на JSP

Web-прототип покрывает базовый клиентский путь:

1. вход по телефону и OTP-коду;
2. просмотр списка слотов;
3. просмотр карточки слота;
4. создание брони;
5. экран успешного бронирования;
6. список «Мои брони»;
7. детали брони;
8. отмена брони;
9. оценка маршала.

JSP-страницы лежат в `src/main/webapp/WEB-INF/jsp/`:

```text
login.jsp
login-code.jsp
slot-list.jsp
slot-card.jsp
booking.jsp
booking-success.jsp
my-bookings.jsp
booking-details.jsp
header.jsp
```

## src/main/resources/ — конфигурация и миграции

| Файл / папка | Назначение |
| --- | --- |
| `application.yml` | Настройки Spring Boot, профили `dev` и `default`, параметры JWT, datasource, JPA и Flyway. |
| `db/V1__init_schema.sql` | SQL-схема приложения для Flyway. |

По умолчанию активен профиль `dev`: приложение использует in-memory H2, создаёт схему через Hibernate и не требует отдельного PostgreSQL.

Для Docker/production-подобного запуска используется профиль `default`: приложение подключается к PostgreSQL и применяет Flyway-миграции.

## database/ — схема БД и Docker-образ PostgreSQL

```text
database/
├── Dockerfile                         # Образ PostgreSQL с init-скриптом
└── migrations/
    └── V1__init_schema.sql            # Основная миграция схемы БД
```

Основные сущности БД:

- `clients` — клиенты;
- `marshals` — маршалы;
- `track_configurations` — конфигурации трассы;
- `slots` — заезды / слоты;
- `bookings` — бронирования;
- `ratings` — оценки маршалов;
- `otp_codes` — OTP-коды;
- `refresh_tokens` — refresh-токены;
- `push_tokens` и `push_notification_logs` — push-инфраструктура;
- `idempotency_keys` — защита создания брони от повторных запросов.

## tests/ — баги и тестовая документация

| Файл | Назначение |
| --- | --- |
| `01bug-report.md`, `02bug-report.md`, `03bag-report.md`, `04bug-report.md` | Отдельные баг-репорты по найденным дефектам. |
| `test-case.md` | Тест-кейсы для проверки пользовательских сценариев. |
| `tests-prompts.md` | Промпты, использованные при подготовке тестовой документации. |

## Файлы в корне

| Файл | Назначение |
| --- | --- |
| `pom.xml` | Maven-проект: Spring Boot 3.1.5, Java 17, Spring Web/Data JPA/Security/Validation/Actuator, PostgreSQL, H2, Flyway, JWT, Springdoc OpenAPI, Lombok, JSP/JSTL, Testcontainers. |
| `docker-compose.yml` | Полный запуск: PostgreSQL, backend и Adminer (через профиль `tools`). |
| `docker-compose.test.yml` | Окружение для тестового запуска. |
| `dockerfile` | Сборка Docker-образа backend-приложения. |
| `implementation-prompts.md` | Лог промптов, использованных при реализации. |

## Как запустить

### Вариант 1. Локально в dev-профиле (H2)

Нужны Java 17 и Maven.

```bash
mvn spring-boot:run
```

После запуска:

- web-прототип: `http://localhost:8080/login`;
- H2 Console: `http://localhost:8080/h2-console`;
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

### Вариант 2. Через Docker Compose (PostgreSQL + backend)

Создайте `.env` с переменными окружения:

```env
POSTGRES_USER=karting
POSTGRES_PASSWORD=1111
POSTGRES_DB=karting
POSTGRES_PORT=5432
BACKEND_PORT=8080
ADMINER_PORT=8081
JWT_SECRET=very_secret_key_for_jwt_must_be_at_least_256_bits
```

Запуск backend и БД:

```bash
docker compose up --build
```

Запуск вместе с Adminer:

```bash
docker compose --profile tools up --build
```

После запуска:

- backend: `http://localhost:8080`;
- web-прототип: `http://localhost:8080/login`;
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`;
- Adminer: `http://localhost:8081`.

## Как быстро найти нужное

| Хочу понять... | Смотреть в |
| --- | --- |
| Зачем этот проект и какая проблема решается | `analysis/1-elicitation/domain-description.md` |
| Что просил заказчик | `analysis/0-customer-brief/brief-karting.md` |
| Какие функции должно делать приложение | `analysis/2-requirements/functional-requirements.md` |
| Какие бизнес-правила и ограничения есть | `analysis/2-requirements/business-requirements.md`, `analysis/2-requirements/non-functional-requirements.md` |
| Как выглядит и работает каждый экран | `analysis/screens/SCR-*.md` |
| Какие модальные окна / состояния есть | `analysis/screens/BS-*.md` |
| Как работает авторизация по OTP | `analysis/09-logic/LOGIC-001-auth-otp.md`, `src/main/java/ru/vsu/cs/yesikov/service/AuthService.java` |
| Как считается доступность слотов | `analysis/09-logic/LOGIC-002-availability-calc.md`, `src/main/java/ru/vsu/cs/yesikov/service/SlotService.java` |
| Как создаётся и отменяется бронь | `analysis/09-logic/LOGIC-003-create-booking.md`, `analysis/09-logic/LOGIC-004-cancel-booking.md`, `src/main/java/ru/vsu/cs/yesikov/service/BookingService.java` |
| Как устроена оценка маршала | `analysis/09-logic/LOGIC-005-marshal-rating.md`, `src/main/java/ru/vsu/cs/yesikov/service/RatingService.java` |
| Контракт API | `analysis/api/openapi.yaml` или Swagger UI после запуска приложения |
| REST-контроллеры | `src/main/java/ru/vsu/cs/yesikov/controller/` |
| Web-интерфейс | `src/main/java/ru/vsu/cs/yesikov/web/WebController.java`, `src/main/webapp/WEB-INF/jsp/` |
| Схему базы данных | `src/main/resources/db/V1__init_schema.sql`, `database/migrations/V1__init_schema.sql` |
| Найденные баги и тест-кейсы | `tests/` |