package com.flux.calendar_service.exceptions;

public class MustBeUniqueException extends RuntimeException {
    public MustBeUniqueException(String message) {
        super(message);
    }
}
