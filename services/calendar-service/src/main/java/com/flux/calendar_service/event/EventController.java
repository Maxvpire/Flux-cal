package com.flux.calendar_service.event;

import com.flux.calendar_service.event.dto.EventRequest;
import com.flux.calendar_service.event.dto.EventResponse;
import com.flux.calendar_service.event.dto.EventUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    /**
     * Create a new event for a specific calendar
     * POST /events/calendar/{calendarId}
     */
    @PostMapping("/calendar/{calendarId}")
    public ResponseEntity<String> createEvent(
            @PathVariable String calendarId,
            @RequestBody @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEvent(calendarId, request));
    }

    @PostMapping("/calendar/{calendarId}/add-meet")
    public ResponseEntity<String> createEventWithMeet(
            @PathVariable String calendarId,
            @RequestBody @Valid EventRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEventWithGoogleMeet(calendarId, request));
    }

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
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Attach an existing location to an event
     * PUT /events/{eventId}/location/{locationId}
     */
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
    @PutMapping("/{eventId}/conference/{conferenceId}")
    public ResponseEntity<?> attachConference(
            @PathVariable String eventId,
            @PathVariable String conferenceId) {
        eventService.attachConference(eventId, conferenceId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
