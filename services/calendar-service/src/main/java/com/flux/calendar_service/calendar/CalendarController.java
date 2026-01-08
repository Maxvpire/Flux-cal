package com.flux.calendar_service.calendar;

import com.flux.calendar_service.calendar.dto.CalendarRequest;
import com.flux.calendar_service.calendar.dto.CalendarResponse;
import com.flux.calendar_service.calendar.dto.CalendarUpdateRequest;
import com.flux.calendar_service.calendar.dto.PrimaryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@RequestMapping("/calendars")
@RequiredArgsConstructor
@Tag(name = "Calendar Controller", description = "Endpoints for managing calendars")
public class CalendarController {
    private final CalendarService calendarService;

    @Operation(summary = "Create a new calendar", description = "Creates a new calendar definition")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Calendar created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid CalendarRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(calendarService.createCalendar(request));
    }

    @Operation(summary = "Get all calendars", description = "Retrieves a list of all calendars")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    public ResponseEntity<List<CalendarResponse>> getAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getAllCalendars());
    }

    @Operation(summary = "Get calendar by ID", description = "Retrieves a specific calendar by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved calendar"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @GetMapping("/id/{id}")
    public ResponseEntity<CalendarResponse> getById(@PathVariable String id){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getCalendarById(id));
    }

    @Operation(summary = "Get calendars by user ID", description = "Retrieves all calendars for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved calendars")
    @GetMapping("/user/id/{id}")
    public ResponseEntity<List<CalendarResponse>> getByUserId(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getCalendarsByUserId(id));
    }

    @Operation(summary = "Get primary calendar", description = "Retrieves the primary calendar for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved primary calendar"),
            @ApiResponse(responseCode = "404", description = "Primary calendar not found")
    })
    @GetMapping("/user/primary/{id}")
    public ResponseEntity<CalendarResponse> getPrimaryCalendar(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getPrimaryCalendar(id));
    }

    @Operation(summary = "Get calendar by title and user", description = "Retrieves a calendar by its title and user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved calendar"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @GetMapping("/title/{title}/user/{id}/")
    public ResponseEntity<CalendarResponse> getByTitle(
            @PathVariable String id,
            @PathVariable String title)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getByTitle(id, title));
    }

    @Operation(summary = "Make calendar primary", description = "Sets a calendar as primary for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Calendar set as primary successfully"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @PutMapping("/user/primary/{id}")
    public ResponseEntity<?> makePrimary(@PathVariable String id, @RequestBody @Valid PrimaryRequest request) {
        calendarService.makePrimary(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Update calendar", description = "Updates an existing calendar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Calendar updated successfully"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody CalendarUpdateRequest request
    ){
        calendarService.updateCalendar(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Recover calendar", description = "Recovers a deleted calendar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Calendar recovered successfully"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @PutMapping("/recover/{id}")
    public ResponseEntity<?> recoverCalendar(@PathVariable String id) {
        calendarService.recoverCalendar(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    };

    @Operation(summary = "Delete calendar", description = "Deletes a calendar by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Calendar deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCalendar(@PathVariable String id) {
        calendarService.deleteCalendar(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}