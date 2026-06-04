package com.example.faketcp.api;

import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, EmptyResultDataAccessException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(Exception e) {
        log.warn("API request failed: {}", e.getMessage(), e);
        return error(e);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(Exception e) {
        log.warn("API request rejected: {}", e.getMessage(), e);
        return error(e);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> duplicateKey(Exception e) {
        log.warn("API data integrity violation", e);
        Map<String, String> body = new HashMap<>();
        body.put("error", "数据唯一性校验失败，请检查 code 或监听端口是否重复");
        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException e) {
        log.warn("API response status exception: {}", e.getReason() == null ? e.getMessage() : e.getReason(), e);
        Map<String, String> body = new HashMap<>();
        String message = e.getReason() == null ? e.getMessage() : e.getReason();
        body.put("error", message);
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    private Map<String, String> error(Exception e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", e.getMessage());
        return body;
    }
}
