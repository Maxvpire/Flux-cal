package com.flux.calendar_service.exceptions;

public class GoogleCalendarDisabledException extends RuntimeException{
    public GoogleCalendarDisabledException(String message) {
        super(message);
    }
}
