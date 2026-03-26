package com.example.keycloakdemo.exception;

import com.example.keycloakdemo.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidToken(InvalidTokenException ex) {
        log.error("invalid_token message={}", ex.getMessage(), ex);
        return ErrorResponse.of("INVALID_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("resource_not_found path={}", ex.getResourcePath());
        return ErrorResponse.of("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다");
    }

    // 내부 예외 메시지를 클라이언트에 직접 노출하지 않음
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        log.error("unexpected_error", ex);
        return ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다");
    }
}
