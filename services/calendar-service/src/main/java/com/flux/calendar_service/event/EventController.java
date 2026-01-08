package com.flux.calendar_service.event;

import com.flux.calendar_service.event.dto.EventRequest;
import com.flux.calendar_service.event.dto.EventResponse;
import com.flux.calendar_service.event.dto.EventUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Event Controller", description = "Endpoints for managing events")
public class EventController {
    private final EventService eventService;

    /**
     * Create a new event for a specific calendar
     * POST /events/calendar/{calendarId}
     */
    @Operation(summary = "Create a new event", description = "Creates a new event in the specified calendar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @PostMapping("/calendar/{calendarId}")
    public ResponseEntity<String> createEvent(
            @PathVariable String calendarId,
            @RequestBody @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEvent(calendarId, request));
    }

    @Operation(summary = "Create event with Google Meet", description = "Creates a new event with a Google Meet link")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/calendar/{calendarId}/add-meet")
    public ResponseEntity<String> createEventWithMeet(
            @PathVariable String calendarId,
            @RequestBody @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEventWithGoogleMeet(calendarId, request));
    }

    @Operation(summary = "Create event with Zoom", description = "Creates a new event with a Zoom meeting link")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/calendar/{calendarId}/add-zoom")
    public ResponseEntity<String> createEventWithZoom(
            @PathVariable String calendarId,
            @RequestBody @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEventWithNewZoomMeeting(calendarId, request));
    }



    /**
     * Get all events
     * GET /events
     */
    @Operation(summary = "Get all events", description = "Retrieves a list of all events")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventService.getAllEvents());
    }

    /**
     * Get event by ID
     * GET /events/{id}
     */
    @Operation(summary = "Get event by ID", description = "Retrieves a specific event by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventService.getEventById(id));
    }

    /**
     * Get all events for a specific calendar
     * GET /events/calendar/{calendarId}
     */
    @Operation(summary = "Get events by calendar", description = "Retrieves all events for a specific calendar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "404", description = "Calendar not found")
    })
    @GetMapping("/calendar/{calendarId}")
    public ResponseEntity<List<EventResponse>> getEventsByCalendar(
            @PathVariable String calendarId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventService.getEventsByCalendarId(calendarId));
    }

    /**
     * Get all events for a specific user (across all their calendars)
     * GET /events/user/{userId}
     */
    @Operation(summary = "Get events by user", description = "Retrieves all events for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved events")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EventResponse>> getEventsByUser(
            @PathVariable String userId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventService.getEventsByUserId(userId));
    }

    /**
     * Update an existing event
     * PUT /events/{id}
     */
    @Operation(summary = "Update event", description = "Updates an existing event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Event updated successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String id,
            @RequestBody EventUpdateRequest request) {
        eventService.updateEvent(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Delete an event
     * DELETE /events/{id}
     */
    @Operation(summary = "Delete event", description = "Deletes an event by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Attach an existing location to an event
     * PUT /events/{eventId}/location/{locationId}
     */
    @Operation(summary = "Attach location", description = "Attaches a location to an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Location attached successfully"),
            @ApiResponse(responseCode = "404", description = "Event or Location not found")
    })
    @PutMapping("/{eventId}/location/{locationId}")
    public ResponseEntity<?> attachLocation(
            @PathVariable String eventId,
            @PathVariable String locationId) {
        eventService.attachLocation(eventId, locationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Attach a conference to an event
     * PUT /events/{eventId}/conference/{conferenceId}
     */
    @Operation(summary = "Attach conference", description = "Attaches a conference to an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Conference attached successfully"),
            @ApiResponse(responseCode = "404", description = "Event or Conference not found")
    })
    @PutMapping("/{eventId}/conference/{conferenceId}")
    public ResponseEntity<?> attachConference(
            @PathVariable String eventId,
            @PathVariable String conferenceId) {
        eventService.attachConference(eventId, conferenceId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
