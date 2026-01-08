package com.flux.calendar_service.exceptions;

public class MustNotBeEmptyException extends RuntimeException{
    public MustNotBeEmptyException(String message){
        super(message);
    }
}
