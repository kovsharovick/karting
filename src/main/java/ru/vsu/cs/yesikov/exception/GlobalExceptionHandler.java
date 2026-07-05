package ru.vsu.cs.yesikov.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.vsu.cs.yesikov.dto.common.ErrorResponse;

import java.sql.SQLException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business error [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("bad_request", message, null));
    }

    /**
     * Прямой java.sql.SQLException (например, из ручного JDBC-кода без прослойки Spring Data).
     * На практике почти все ошибки триггеров БД доходят через DataAccessException ниже,
     * этот обработчик — на случай прямых вызовов JDBC.
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQL(SQLException ex) {
        BusinessException mapped = SqlErrorMapper.toBusinessException(ex);
        log.warn("SQL error mapped to [{}]: {}", mapped.getCode(), ex.getMessage());
        return ResponseEntity
                .status(mapped.getStatus())
                .body(new ErrorResponse(mapped.getCode(), mapped.getMessage(), mapped.getDetails()));
    }

    /**
     * Основной путь для ошибок триггеров БД (P0001-P0006) и unique_violation (23505):
     * Spring Data/Hibernate оборачивает исходный SQLException в DataAccessException
     * (DataIntegrityViolationException, JpaSystemException и т.п.), поэтому именно этот
     * обработчик ловит бизнес-конфликты, если сервис не перехватил их сам явным try/catch.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        BusinessException mapped = SqlErrorMapper.toBusinessException(ex);
        log.warn("DataAccessException mapped to [{}]: {}", mapped.getCode(), ex.getMessage());
        return ResponseEntity
                .status(mapped.getStatus())
                .body(new ErrorResponse(mapped.getCode(), mapped.getMessage(), mapped.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("internal_error", "Something went wrong", null));
    }
}