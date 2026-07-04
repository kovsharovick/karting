package ru.vsu.cs.yesikov.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(String message, HttpStatus status, String code) {
        this(message, status, code, null);
    }

    public BusinessException(String message, HttpStatus status, String code, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
}