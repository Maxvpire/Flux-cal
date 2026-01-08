package com.flux.calendar_service.exceptions;

public class ZoomMeetingFailedException extends RuntimeException{
    public ZoomMeetingFailedException(String message) {
        super(message);
    }
}