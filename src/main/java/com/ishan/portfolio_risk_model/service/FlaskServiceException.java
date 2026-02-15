package com.ishan.portfolio_risk_model.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FlaskServiceException extends RuntimeException {

    private final HttpStatus status;

    public FlaskServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
