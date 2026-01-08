package com.flux.calendar_service.exceptions;

public class MinIoBucketInitializingErrorException extends RuntimeException{
    public MinIoBucketInitializingErrorException(String message) {
        super(message);
    }
}
