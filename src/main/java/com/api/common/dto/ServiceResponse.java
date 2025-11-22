package com.api.common.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ServiceResponse<T> {

    private final HttpStatus status;
    private final T body;

    private ServiceResponse(HttpStatus status, T body) {
        this.status = status;
        this.body = body;
    }

    public static <T> ServiceResponse<T> ok(T body) {
        return new ServiceResponse<>(HttpStatus.OK, body);
    }

    public static <T> ServiceResponse<T> of(HttpStatus status, T body) {
        return new ServiceResponse<>(status, body);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public T getBody() {
        return body;
    }

    public ResponseEntity<T> toResponseEntity() {
        return ResponseEntity.status(status).body(body);
    }
}

