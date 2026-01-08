package com.flux.calendar_service.conference;

import com.flux.calendar_service.google.GoogleCalendarApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/conference")
@RequiredArgsConstructor
@Tag(name = "Conference Controller", description = "Endpoints for managing conferences")
public class ConferenceController {
    private final com.flux.calendar_service.event.EventService eventService;

    @Operation(summary = "Add Google Meet", description = "Adds a Google Meet link to an existing event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Google Meet added successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PostMapping("/{id}/add-meet")
    public org.springframework.http.ResponseEntity<Void> addGoogleMeet(@PathVariable String id) {
        eventService.addGoogleMeetToExistingEvent(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Operation(summary = "Add Zoom Meeting", description = "Adds a Zoom meeting link to an existing event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Zoom meeting added successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PostMapping("/{id}/add-zoom")
    public ResponseEntity<Void> addZoomToEvent(@PathVariable String id) {
        eventService.addZoomToExistingEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Remove Google Meet", description = "Removes a Google Meet link from an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Google Meet removed successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}/remove-meet")
    public org.springframework.http.ResponseEntity<Void> removeGoogleMeet(@PathVariable String id) {
        eventService.removeGoogleMeetFromEvent(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove Zoom Meeting", description = "Removes a Zoom meeting from an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Zoom meeting removed successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}/remove-zoom")
    public ResponseEntity<Void> removeZoomFromEvent(@PathVariable String id) {
        eventService.removeZoomFromEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
