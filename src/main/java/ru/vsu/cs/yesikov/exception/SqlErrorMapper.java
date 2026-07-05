package ru.vsu.cs.yesikov.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Единая точка маппинга ошибок БД (SQLSTATE триггеров P0001-P0006, unique_violation 23505)
 * на бизнес-коды контракта API (openapi.yaml → components.responses).
 * <p>
 * Нужен, потому что Spring Data/Hibernate оборачивает исходный java.sql.SQLException в
 * DataAccessException (DataIntegrityViolationException, JpaSystemException и т.п.) —
 * простой @ExceptionHandler(SQLException.class) такие ошибки не ловит.
 */
public final class SqlErrorMapper {

    private SqlErrorMapper() {
    }

    public static BusinessException toBusinessException(DataAccessException ex) {
        Optional<SQLException> sqlEx = findSqlException(ex);
        if (sqlEx.isEmpty()) {
            return new BusinessException("Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
        }
        return mapSqlState(sqlEx.get());
    }

    public static BusinessException toBusinessException(SQLException ex) {
        return mapSqlState(ex);
    }

    private static BusinessException mapSqlState(SQLException ex) {
        String sqlState = ex.getSQLState();
        String message = ex.getMessage() == null ? "" : ex.getMessage();

        // Триггеры БД (database/migrations/V1__init_schema.sql, §7.1, §8)
        if ("P0001".equals(sqlState)) {
            return new BusinessException("Заезд отменён администрацией", HttpStatus.GONE, "slot_cancelled");
        }
        if ("P0002".equals(sqlState)) {
            return new BusinessException("Свободных мест больше нет", HttpStatus.CONFLICT, "slot_full");
        }
        if ("P0003".equals(sqlState)) {
            return new BusinessException("Недостаточно прокатной экипировки", HttpStatus.CONFLICT, "gear_unavailable");
        }
        if ("P0004".equals(sqlState) || "P0006".equals(sqlState)) {
            return new BusinessException("Доступ запрещён", HttpStatus.FORBIDDEN, "forbidden");
        }
        if ("P0005".equals(sqlState)) {
            return new BusinessException("Операция недоступна", HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable");
        }

        // Уникальные ограничения БД
        if ("23505".equals(sqlState) && message.contains("uq_bookings_active_per_client_slot")) {
            return new BusinessException("У вас уже есть активная запись на этот заезд",
                    HttpStatus.CONFLICT, "double_booking");
        }
        if ("23505".equals(sqlState) && message.contains("uq_ratings_booking")) {
            return new BusinessException("Оценка уже оставлена", HttpStatus.CONFLICT, "conflict");
        }

        return new BusinessException("Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
    }

    private static Optional<SQLException> findSqlException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException se) {
                return Optional.of(se);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}