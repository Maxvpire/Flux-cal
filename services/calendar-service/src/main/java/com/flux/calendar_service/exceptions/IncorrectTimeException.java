package com.flux.calendar_service.exceptions;

public class IncorrectTimeException extends RuntimeException {
    public IncorrectTimeException(String message) {
        super(message);
    }
}
