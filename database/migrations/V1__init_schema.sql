-- =====================================================================
-- «Апекс» — картинг-центр. Схема БД клиентского API.
-- =====================================================================
-- Источники: analysis/1-elicitation/domain-description.md,
--            analysis/4-design/data-model.md,
--            analysis/api/openapi.yaml,
--            analysis/09-logic/LOGIC-001..006-*.md
--
-- СУБД:   PostgreSQL 15+
--
-- Файл — первая (и пока единственная) миграция. Имя V1__init_schema.sql
-- соответствует конвенции Flyway (V<версия>__<описание>.sql), поэтому
-- один и тот же файл используется двумя способами без изменений:
--   1) как init-скрипт Docker-образа (COPY в /docker-entrypoint-initdb.d/,
--      см. database/Dockerfile) — выполняется один раз при первом старте
--      контейнера на пустом PGDATA;
--   2) как src/main/resources/db/migration/V1__init_schema.sql в
--      Java/Kotlin-приложении (Spring Boot + Flyway) — когда миграциями
--      начинает управлять сам бэкенд, а не Docker-образ БД (см.
--      database/README.md, раздел «Кто владеет миграциями»).
-- Явных BEGIN/COMMIT в файле нет: Flyway сам оборачивает каждую миграцию
-- в транзакцию, а psql (init-скрипт) корректно выполняет DDL и без них.
--
-- Скоуп: реализована модель, обслуживающая контракт клиентского API
-- (openapi.yaml). Формирование расписания (создание Slot/TrackConfiguration/
-- Marshal) — прерогатива существующей инфраструктуры (R-004, R-015, R-028) и
-- в клиентское приложение не входит, но таблицы включены в схему, т.к. это
-- source of truth, с которым интегрируется API.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0. Расширения и служебные функции
-- ---------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid()

-- Универсальный триггер обновления updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- 1. ENUM-типы (соответствуют enum'ам openapi.yaml)
-- ---------------------------------------------------------------------

CREATE TYPE track_config_type AS ENUM ('novice', 'experienced');
-- novice — короткая (до 8 чел.), experienced — длинная (до 14 по картам)

CREATE TYPE slot_status AS ENUM ('scheduled', 'cancelled');
-- cancelled — заезд отменён центром (R-008)

CREATE TYPE booking_status AS ENUM ('active', 'cancelled', 'late_cancel', 'center_cancelled');
-- см. data-model.md → «Жизненный цикл бронирования»

CREATE TYPE push_platform AS ENUM ('ios', 'android');

CREATE TYPE otp_purpose AS ENUM ('login', 'phone_change');
-- login — /auth/request-code, phone_change — /profile/phone/request-code


-- =====================================================================
-- 2. КЛИЕНТ (Client) — управляется приложением
-- =====================================================================
-- LOGIC-001-auth-otp.md: вход по телефону + SMS OTP, без паролей.

CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone       VARCHAR(20) NOT NULL,
    name        VARCHAR(100),                 -- пусто у нового клиента (is_new = true)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,                  -- soft delete: DELETE /profile освобождает телефон

    -- Формат E.164: ^\+[1-9]\d{1,14}$ (openapi.yaml)
    CONSTRAINT chk_clients_phone_e164 CHECK (phone ~ '^\+[1-9][0-9]{1,14}$')
);

-- Телефон уникален только среди «живых» аккаунтов — после удаления
-- (DELETE /profile) он должен освобождаться для повторной регистрации (R-...).
CREATE UNIQUE INDEX uq_clients_phone_active
    ON clients (phone)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_clients_updated_at
    BEFORE UPDATE ON clients
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 3. СПРАВОЧНИК: КОНФИГУРАЦИЯ ТРАССЫ (TrackConfiguration) — read-only для клиента
-- =====================================================================
-- domain-description.md: короткая (novice, потолок 8 чел.) / длинная (experienced).

CREATE TABLE track_configurations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL,          -- 'Короткая' | 'Длинная'
    type            track_config_type NOT NULL UNIQUE,
    description     TEXT,
    max_group_size  SMALLINT NOT NULL,             -- бизнес-потолок группы для типа трассы
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_track_cfg_group_size CHECK (
        (type = 'novice'      AND max_group_size BETWEEN 1 AND 8)  OR
        (type = 'experienced' AND max_group_size BETWEEN 1 AND 14)
    )
);

CREATE TRIGGER trg_track_configurations_updated_at
    BEFORE UPDATE ON track_configurations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Справочник фиксирован доменом — засеиваем сразу.
INSERT INTO track_configurations (name, type, description, max_group_size) VALUES
    ('Короткая', 'novice',      'Новичковая трасса, часть картов с ограничением скорости', 8),
    ('Длинная',  'experienced', 'Трасса для опытных райдеров', 14);


-- =====================================================================
-- 4. СПРАВОЧНИК: МАРШАЛ (Marshal) — read-only для клиента
-- =====================================================================
-- rating_avg денормализован и поддерживается триггером на ratings (см. §8).

CREATE TABLE marshals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT true,     -- сезонные подработчики (бриф Дениса)
    rating_avg  NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    rating_count INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_marshals_rating_avg CHECK (rating_avg BETWEEN 0 AND 5)
);

CREATE TRIGGER trg_marshals_updated_at
    BEFORE UPDATE ON marshals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =====================================================================
-- 5. СЛОТ / ЗАЕЗД (Slot) — формируется существующей инфраструктурой,
--    read-only для клиентского приложения (R-004, R-015, R-028)
-- =====================================================================
-- free_karts / free_rental_gear денормализованы для быстрых выборок
-- listSlots/getSlot и поддерживаются транзакционно триггерами на bookings
-- (см. §7) — но истинный инвариант (data-model.md §«Ключевые инварианты»)
-- всегда можно пересчитать через VIEW slot_availability_recalculated.

CREATE TABLE slots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    track_config_id     UUID NOT NULL REFERENCES track_configurations(id),
    marshal_id          UUID NOT NULL REFERENCES marshals(id),

    start_at            TIMESTAMPTZ NOT NULL,          -- источник истины для правила «1 час»
    duration_minutes    SMALLINT NOT NULL DEFAULT 20,  -- заезд + инструктаж (LOGIC-005)

    total_karts         SMALLINT NOT NULL,
    free_karts          SMALLINT NOT NULL,             -- денормализованный остаток
    total_rental_gear   SMALLINT NOT NULL DEFAULT 0,
    free_rental_gear    SMALLINT NOT NULL,              -- денормализованный остаток

    price_kart          INTEGER NOT NULL,               -- RUB, за место
    price_gear_rental   INTEGER NOT NULL,               -- RUB, за комплект

    requirements_text   TEXT,                           -- возраст/рост, инфо-предупреждение
    meeting_point       TEXT,                           -- foundations §4.4
    address             TEXT,

    status              slot_status NOT NULL DEFAULT 'scheduled',
    cancellation_reason TEXT,                           -- заполняется при отмене центром/погодой

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_slots_total_karts       CHECK (total_karts BETWEEN 1 AND 14),
    CONSTRAINT chk_slots_free_karts        CHECK (free_karts BETWEEN 0 AND total_karts),
    CONSTRAINT chk_slots_total_rental_gear CHECK (total_rental_gear >= 0),
    CONSTRAINT chk_slots_free_rental_gear  CHECK (free_rental_gear BETWEEN 0 AND total_rental_gear),
    CONSTRAINT chk_slots_prices            CHECK (price_kart >= 0 AND price_gear_rental >= 0),
    CONSTRAINT chk_slots_duration          CHECK (duration_minutes > 0),
    CONSTRAINT chk_slots_cancellation_reason CHECK (
        (status = 'cancelled' AND cancellation_reason IS NOT NULL) OR
        (status = 'scheduled')
    )
);

CREATE INDEX idx_slots_start_at            ON slots (start_at);
CREATE INDEX idx_slots_track_config_id     ON slots (track_config_id);
CREATE INDEX idx_slots_marshal_id          ON slots (marshal_id);
CREATE INDEX idx_slots_status_start_at     ON slots (status, start_at);
-- Основной паттерн listSlots: диапазон дат + фильтры + only_available
CREATE INDEX idx_slots_listing ON slots (start_at) INCLUDE (free_karts, status);

CREATE TRIGGER trg_slots_updated_at
    BEFORE UPDATE ON slots
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Триггер: total_karts новичковой (novice) конфигурации не может превышать
-- потолок max_group_size конфигурации (кросс-табличное правило, CHECK
-- не подходит — нужен join). REQ-FUNC-BOOK-001, LOGIC-002 шаг 1.
CREATE OR REPLACE FUNCTION check_slot_kart_cap()
RETURNS TRIGGER AS $$
DECLARE
    v_cap SMALLINT;
BEGIN
    SELECT max_group_size INTO v_cap
    FROM track_configurations
    WHERE id = NEW.track_config_id;

    IF NEW.total_karts > v_cap THEN
        RAISE EXCEPTION
            'total_karts (%) превышает потолок конфигурации трассы (%)',
            NEW.total_karts, v_cap;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_slots_check_kart_cap
    BEFORE INSERT OR UPDATE OF total_karts, track_config_id ON slots
    FOR EACH ROW EXECUTE FUNCTION check_slot_kart_cap();


-- =====================================================================
-- 6. ИДЕМПОТЕНТНОСТЬ ЗАПРОСОВ (Idempotency-Key) — NFR-9, LOGIC-003 шаг 1
-- =====================================================================
-- Обязательный заголовок Idempotency-Key для POST /bookings: повторный
-- запрос с тем же ключом не должен породить вторую бронь
-- (double_booking / idempotency_key_conflict, openapi.yaml).

CREATE TABLE idempotency_keys (
    idempotency_key   UUID NOT NULL,
    client_id         UUID NOT NULL REFERENCES clients(id),
    endpoint          VARCHAR(100) NOT NULL,        -- напр. 'POST /bookings'
    request_fingerprint TEXT NOT NULL,               -- hash тела запроса — для idempotency_key_conflict
    response_status   SMALLINT,
    response_body     JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (idempotency_key, client_id, endpoint)
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);
-- (в проде — периодическая очистка старых записей job'ом/TTL)


-- =====================================================================
-- 7. БРОНЬ (Booking) — управляется приложением
-- =====================================================================

CREATE TABLE bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id             UUID NOT NULL REFERENCES slots(id),
    client_id           UUID NOT NULL REFERENCES clients(id),

    seats_count         SMALLINT NOT NULL,
    rental_gear_count   SMALLINT NOT NULL,

    status              booking_status NOT NULL DEFAULT 'active',
    price_total         INTEGER NOT NULL,               -- считается сервером атомарно
    cancellation_reason TEXT,                            -- только для center_cancelled (наследуется от slot)

    idempotency_key     UUID,                            -- ключ, которым создана бронь (аудит)

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at         TIMESTAMPTZ,

    CONSTRAINT chk_bookings_seats_count       CHECK (seats_count BETWEEN 1 AND 14),
    CONSTRAINT chk_bookings_rental_gear_count CHECK (
        rental_gear_count BETWEEN 0 AND seats_count
    ),
    CONSTRAINT chk_bookings_price_total       CHECK (price_total >= 0),
    CONSTRAINT chk_bookings_cancelled_at      CHECK (
        (status = 'active' AND cancelled_at IS NULL) OR
        (status <> 'active' AND cancelled_at IS NOT NULL)
    )
);

CREATE INDEX idx_bookings_client_id          ON bookings (client_id);
CREATE INDEX idx_bookings_slot_id            ON bookings (slot_id);
CREATE INDEX idx_bookings_client_status       ON bookings (client_id, status);
-- Быстрая проверка double_booking: активная бронь клиента на конкретный слот
CREATE UNIQUE INDEX uq_bookings_active_per_client_slot
    ON bookings (client_id, slot_id)
    WHERE status = 'active';

-- ---------------------------------------------------------------------
-- 7.1 Резервирование инвентаря при создании брони (атомарно, R-004)
-- ---------------------------------------------------------------------
-- Блокировка строки slot (FOR UPDATE через SELECT ... FOR UPDATE в самом
-- триггере посредством UPDATE ... WHERE) + проверка остатков в одной
-- транзакции гарантирует «0 двойных броней» (NFR-3) на уровне БД —
-- дополнительно к бизнес-логике на уровне приложения/Java-сервиса.

CREATE OR REPLACE FUNCTION reserve_slot_inventory()
RETURNS TRIGGER AS $$
DECLARE
    v_free_karts        SMALLINT;
    v_free_rental_gear  SMALLINT;
    v_slot_status       slot_status;
BEGIN
    SELECT free_karts, free_rental_gear, status
    INTO v_free_karts, v_free_rental_gear, v_slot_status
    FROM slots
    WHERE id = NEW.slot_id
    FOR UPDATE;

    IF v_slot_status = 'cancelled' THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0001',
            MESSAGE  = 'slot_cancelled: заезд отменён администрацией';
    END IF;

    IF v_free_karts < NEW.seats_count THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0002',
            MESSAGE  = format('slot_full: доступно мест %s', v_free_karts);
    END IF;

    IF v_free_rental_gear < NEW.rental_gear_count THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0003',
            MESSAGE  = format('gear_unavailable: доступно комплектов %s', v_free_rental_gear);
    END IF;

    UPDATE slots
    SET free_karts       = free_karts - NEW.seats_count,
        free_rental_gear = free_rental_gear - NEW.rental_gear_count
    WHERE id = NEW.slot_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bookings_reserve_inventory
    BEFORE INSERT ON bookings
    FOR EACH ROW
    WHEN (NEW.status = 'active')
    EXECUTE FUNCTION reserve_slot_inventory();

-- ---------------------------------------------------------------------
-- 7.2 Освобождение инвентаря при отмене (только ранняя отмена — R-...)
-- ---------------------------------------------------------------------
-- data-model.md: только `cancelled` (ранняя, >= 1 ч) возвращает места и
-- экипировку в фонд; `late_cancel` удерживает их (без штрафов для клиента).
-- Тип отмены (ранняя/поздняя) определяется приложением на основании
-- serверного now() vs slot.start_at перед UPDATE — здесь только эффект
-- на инвентарь по итоговому статусу.

CREATE OR REPLACE FUNCTION release_slot_inventory_on_cancel()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'active' AND NEW.status = 'cancelled' THEN
        UPDATE slots
        SET free_karts       = free_karts + OLD.seats_count,
            free_rental_gear = free_rental_gear + OLD.rental_gear_count
        WHERE id = OLD.slot_id;
    END IF;
    -- late_cancel / center_cancelled: инвентарь НЕ освобождается
    -- (data-model.md → «Бизнес-логика изменения состояний»).
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bookings_release_inventory
    AFTER UPDATE OF status ON bookings
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION release_slot_inventory_on_cancel();


-- =====================================================================
-- 8. ОЦЕНКА МАРШАЛА (Rating) — управляется приложением
-- =====================================================================
-- LOGIC-005: необязательная, однократная (без редактирования), доступна
-- только после завершённого заезда (completed_locally на клиенте,
-- инвариант на сервере — data-model.md §«Ключевые инварианты» п.4).

CREATE TABLE ratings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID NOT NULL REFERENCES clients(id),
    marshal_id  UUID NOT NULL REFERENCES marshals(id),
    booking_id  UUID NOT NULL REFERENCES bookings(id),
    value       SMALLINT NOT NULL,
    comment     VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_ratings_value CHECK (value BETWEEN 1 AND 5),
    -- Оценка может быть оставлена только один раз для одной брони
    CONSTRAINT uq_ratings_booking UNIQUE (booking_id)
);

CREATE INDEX idx_ratings_marshal_id ON ratings (marshal_id);
CREATE INDEX idx_ratings_client_id  ON ratings (client_id);

-- Оценка допустима только для брони со статусом active и только после
-- фактического завершения заезда (старт + длительность < now()).
CREATE OR REPLACE FUNCTION check_rating_eligibility()
RETURNS TRIGGER AS $$
DECLARE
    v_status      booking_status;
    v_start_at    TIMESTAMPTZ;
    v_duration    SMALLINT;
    v_client_id   UUID;
    v_marshal_id  UUID;
BEGIN
    SELECT b.status, s.start_at, s.duration_minutes, b.client_id, s.marshal_id
    INTO v_status, v_start_at, v_duration, v_client_id, v_marshal_id
    FROM bookings b
    JOIN slots s ON s.id = b.slot_id
    WHERE b.id = NEW.booking_id;

    IF v_status <> 'active' THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0004',
            MESSAGE = 'forbidden: бронь не активна — оценка недоступна';
    END IF;

    IF now() < v_start_at + make_interval(mins => v_duration) THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0005',
            MESSAGE = 'unprocessable: заезд ещё не завершён';
    END IF;

    IF NEW.client_id <> v_client_id THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0004',
            MESSAGE = 'forbidden: бронь принадлежит другому клиенту';
    END IF;

    IF NEW.marshal_id <> v_marshal_id THEN
        RAISE EXCEPTION USING
            ERRCODE = 'P0006',
            MESSAGE = 'bad_request: маршал не соответствует брони';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ratings_check_eligibility
    BEFORE INSERT ON ratings
    FOR EACH ROW EXECUTE FUNCTION check_rating_eligibility();

-- Пересчёт rating_avg/rating_count маршала после каждой новой оценки.
CREATE OR REPLACE FUNCTION recalc_marshal_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE marshals m
    SET rating_avg = sub.avg_value,
        rating_count = sub.cnt
    FROM (
        SELECT marshal_id, ROUND(AVG(value)::numeric, 2) AS avg_value, COUNT(*) AS cnt
        FROM ratings
        WHERE marshal_id = NEW.marshal_id
        GROUP BY marshal_id
    ) sub
    WHERE m.id = sub.marshal_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ratings_recalc_marshal_rating
    AFTER INSERT ON ratings
    FOR EACH ROW EXECUTE FUNCTION recalc_marshal_rating();


-- =====================================================================
-- 9. АВТОРИЗАЦИЯ: OTP-КОДЫ (LOGIC-001, шаг 1-2)
-- =====================================================================
-- Код хранится только как хэш (никогда plaintext) + защита от перебора
-- через attempts/expires_at (429 too_many_requests обрабатывается уровнем
-- приложения/rate-limiter, здесь — данные для его работы).

CREATE TABLE otp_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20) NOT NULL,
    purpose         otp_purpose NOT NULL DEFAULT 'login',
    code_hash       TEXT NOT NULL,
    attempts        SMALLINT NOT NULL DEFAULT 0,
    max_attempts    SMALLINT NOT NULL DEFAULT 5,
    expires_at      TIMESTAMPTZ NOT NULL,
    resend_after    TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,               -- код использован (успешная verify-code)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_otp_codes_phone_e164 CHECK (phone ~ '^\+[1-9][0-9]{1,14}$'),
    CONSTRAINT chk_otp_codes_attempts   CHECK (attempts >= 0 AND attempts <= max_attempts)
);

CREATE INDEX idx_otp_codes_phone_active ON otp_codes (phone, expires_at)
    WHERE consumed_at IS NULL;


-- =====================================================================
-- 10. СЕССИИ: REFRESH-ТОКЕНЫ (LOGIC-001, шаг 3-4)
-- =====================================================================
-- access_token — короткоживущий JWT, не хранится в БД (валидируется по
-- подписи). refresh_token хранится как хэш с ротацией при обновлении.

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id),
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,                -- инвалидация при logout / ротации / компрометации
    replaced_by     UUID REFERENCES refresh_tokens(id), -- цепочка ротации
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_client_id ON refresh_tokens (client_id);
CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens (client_id)
    WHERE revoked_at IS NULL;


-- =====================================================================
-- 11. PUSH-ТОКЕНЫ УСТРОЙСТВ (LOGIC-006)
-- =====================================================================

CREATE TABLE push_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID NOT NULL REFERENCES clients(id),
    token       TEXT NOT NULL,
    platform    push_platform NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_push_tokens_token_platform UNIQUE (token, platform)
);

CREATE INDEX idx_push_tokens_client_id ON push_tokens (client_id);


-- =====================================================================
-- 12. ЖУРНАЛ ИСХОДЯЩИХ PUSH (вспомогательно, для FR-20/FR-21/AC LOGIC-006)
-- =====================================================================
-- Не в исходном data-model.md, но необходим для идемпотентной отправки
-- напоминаний / уведомлений об отмене центром (чтобы не задублировать push
-- при повторном запуске джобы напоминаний).

CREATE TYPE push_notification_type AS ENUM ('reminder', 'center_cancelled');

CREATE TABLE push_notifications_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID NOT NULL REFERENCES clients(id),
    booking_id  UUID REFERENCES bookings(id),
    type        push_notification_type NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- одно и то же событие для одной брони не должно отправляться дважды
    CONSTRAINT uq_push_log_booking_type UNIQUE (booking_id, type)
);


-- =====================================================================
-- 13. VIEW: пересчёт доступности слота «с нуля» (сверка/аудит)
-- =====================================================================
-- Соответствует data-model.md → «Ключевые инварианты целостности данных»
-- п.1 и п.3 — независимая проверка денормализованных free_karts/
-- free_rental_gear в slots. Используется для фоновой сверки/мониторинга,
-- не в горячем пути API.

CREATE VIEW slot_availability_recalculated AS
SELECT
    s.id AS slot_id,
    s.total_karts,
    s.total_karts - COALESCE(SUM(b.seats_count)
        FILTER (WHERE b.status IN ('active', 'late_cancel')), 0) AS free_karts_recalculated,
    s.total_rental_gear,
    s.total_rental_gear - COALESCE(SUM(b.rental_gear_count)
        FILTER (WHERE b.status IN ('active', 'late_cancel')), 0) AS free_rental_gear_recalculated
FROM slots s
LEFT JOIN bookings b ON b.slot_id = s.id
GROUP BY s.id, s.total_karts, s.total_rental_gear;


-- =====================================================================
-- 14. VIEW: определение «заезд завершён» для оценки (серверный аналог
--     completed_locally из LOGIC-005, используется вместо клиентского
--     эвристического расчёта — рекомендация REQ-DATA-RATE-001)
-- =====================================================================

CREATE VIEW booking_completion_status AS
SELECT
    b.id AS booking_id,
    b.status,
    s.start_at + make_interval(mins => s.duration_minutes) AS completes_at,
    (b.status = 'active'
        AND now() >= s.start_at + make_interval(mins => s.duration_minutes)
    ) AS is_completed,
    (r.id IS NOT NULL) AS has_rating
FROM bookings b
JOIN slots s ON s.id = b.slot_id
LEFT JOIN ratings r ON r.booking_id = b.id;


-- =====================================================================
-- Примечания по реализации (Java/Kotlin + PostgreSQL + Docker)
-- =====================================================================
-- 1. Бизнес-исключения из триггеров (ERRCODE P0001..P0006) следует
--    перехватывать в data-access слое (напр. Spring Data JPA / Exposed /
--    jOOQ) и мапить на коды ошибок openapi.yaml:
--      P0001 -> 410 Gone            (slot_cancelled)
--      P0002 -> 409 Conflict        (slot_full)
--      P0003 -> 409 Conflict        (gear_unavailable)
--      P0004 -> 403 Forbidden
--      P0005 -> 422 Unprocessable   (заезд не завершён / уже начался)
--      P0006 -> 400 Bad Request
--    Дублирующая активная бронь (uq_bookings_active_per_client_slot) даёт
--    стандартную ошибку unique_violation (23505) -> 409 double_booking.
-- 2. Изоляция транзакций: READ COMMITTED достаточно благодаря явному
--    SELECT ... FOR UPDATE в reserve_slot_inventory(); при желании можно
--    поднять до REPEATABLE READ на уровне сервиса создания брони.
-- 3. otp_codes.code_hash — использовать bcrypt/argon2 на стороне
--    приложения (Kotlin), не pgcrypto digest() с солью по умолчанию.
-- 4. Рекомендуемая миграционная обвязка: Flyway/Liquibase поверх этого
--    файла как V1__init_schema.sql в Docker-образе приложения.
-- =====================================================================
