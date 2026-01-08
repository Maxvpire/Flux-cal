package com.flux.calendar_service.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.flux.calendar_service.exceptions.AddGoogleMeetFailedException;
import com.flux.calendar_service.exceptions.ConflictException;
import com.flux.calendar_service.exceptions.EmptyCalendarsException;
import com.flux.calendar_service.exceptions.GoogleCalendarDisabledException;
import com.flux.calendar_service.exceptions.GoogleCalendarSyncFailedException;
import com.flux.calendar_service.exceptions.IncorrectTimeException;
import com.flux.calendar_service.exceptions.MinIoBucketInitializingErrorException;
import com.flux.calendar_service.exceptions.MinIoDeleteErrorException;
import com.flux.calendar_service.exceptions.MinIoRetrievingErrorException;
import com.flux.calendar_service.exceptions.MinIoUploadingErrorException;
import com.flux.calendar_service.exceptions.MustBeUniqueException;
import com.flux.calendar_service.exceptions.MustNotBeEmptyException;
import com.flux.calendar_service.exceptions.RemoveGoogleMeetFailedException;
import com.flux.calendar_service.exceptions.SomethingWentWrongException;
import com.flux.calendar_service.exceptions.ZoomCredentionalsNotFullyConfiguredException;
import com.flux.calendar_service.exceptions.ZoomMeetingFailedException;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MustBeUniqueException.class)
    public ResponseEntity<ErrorResponse> handleMustBeUniqueException(MustBeUniqueException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    
    @ExceptionHandler(SomethingWentWrongException.class)
    public ResponseEntity<ErrorResponse> handleSomethingWentWrongException(SomethingWentWrongException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MustNotBeEmptyException.class)
    public ResponseEntity<ErrorResponse> handleMustNotBeEmptyException(MustNotBeEmptyException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(EmptyCalendarsException.class)
    public ResponseEntity<ErrorResponse> handleEmptyCalendarsException(EmptyCalendarsException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(IncorrectTimeException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectTimeException(IncorrectTimeException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(GoogleCalendarDisabledException.class) 
    public ResponseEntity<ErrorResponse> handleGoogleCalendarDisabledException(GoogleCalendarDisabledException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(GoogleCalendarSyncFailedException.class)
    public ResponseEntity<ErrorResponse> handleGoogleCalendarSyncFailedException(GoogleCalendarSyncFailedException exp){
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(AddGoogleMeetFailedException.class)
    public ResponseEntity<ErrorResponse> handleAddGoogleMeetFailedException(AddGoogleMeetFailedException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }
    
    @ExceptionHandler(RemoveGoogleMeetFailedException.class)
    public ResponseEntity<ErrorResponse> handleRemoveGoogleMeetFailedException(RemoveGoogleMeetFailedException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> ConflictException(ConflictException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MinIoBucketInitializingErrorException.class)
    public ResponseEntity<ErrorResponse> handleMinIoBucketInitializingErrorException(MinIoBucketInitializingErrorException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MinIoRetrievingErrorException.class)
    public ResponseEntity<ErrorResponse> handleMinIoRetrievingErrorException(MinIoRetrievingErrorException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MinIoUploadingErrorException.class)
    public ResponseEntity<ErrorResponse> handleMinIoUploadingErrorException(MinIoUploadingErrorException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MinIoDeleteErrorException.class)
    public ResponseEntity<ErrorResponse> handleMinIoDeleteErrorException(MinIoDeleteErrorException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(ZoomMeetingFailedException.class)
    public ResponseEntity<ErrorResponse> handleZoomMeetingFailedException(ZoomMeetingFailedException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        //Send natification to slack about error!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(ZoomCredentionalsNotFullyConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleZoomCredentionalsNotFullyConfiguredException(ZoomCredentionalsNotFullyConfiguredException exp) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", exp.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
          .getFieldErrors()
          .forEach(error ->
              errors.put(error.getField(), error.getDefaultMessage())
          );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {

        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());
        log.error("Unhandled exception occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errors));
    }
}