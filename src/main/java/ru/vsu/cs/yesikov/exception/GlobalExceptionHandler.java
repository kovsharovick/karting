package ru.vsu.cs.yesikov.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.vsu.cs.yesikov.dto.common.ErrorResponse;

import java.sql.SQLException;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
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

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQL(SQLException ex) {
        // Маппинг кодов ошибок БД (P0001-P0006) на бизнес-коды
        String sqlState = ex.getSQLState();
        if ("P0001".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ErrorResponse("slot_cancelled", ex.getMessage(), null));
        } else if ("P0002".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("slot_full", ex.getMessage(), null));
        } else if ("P0003".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("gear_unavailable", ex.getMessage(), null));
        } else if ("P0004".equals(sqlState) || "P0006".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("forbidden", ex.getMessage(), null));
        } else if ("P0005".equals(sqlState)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("unprocessable", ex.getMessage(), null));
        } else if ("23505".equals(sqlState) && ex.getMessage().contains("uq_bookings_active_per_client_slot")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("double_booking", "У вас уже есть активная бронь на этот заезд", null));
        }
        log.error("SQL error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("internal_error", "Internal server error", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("internal_error", "Something went wrong", null));
    }
}