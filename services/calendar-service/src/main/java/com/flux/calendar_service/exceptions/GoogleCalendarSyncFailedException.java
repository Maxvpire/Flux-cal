package com.flux.calendar_service.exceptions;

public class GoogleCalendarSyncFailedException extends RuntimeException{
    public GoogleCalendarSyncFailedException(String message) {
        super(message);
    }
}
