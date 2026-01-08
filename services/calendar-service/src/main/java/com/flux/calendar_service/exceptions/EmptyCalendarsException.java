package com.flux.calendar_service.exceptions;

public class EmptyCalendarsException extends RuntimeException {
    public EmptyCalendarsException(String message) {
        super(message);
    }
}
