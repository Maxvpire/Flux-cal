package com.flux.calendar_service.conference;

import com.flux.calendar_service.google.GoogleCalendarApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conference")
@RequiredArgsConstructor
public class ConferenceController {
    private final com.flux.calendar_service.event.EventService eventService;

    @PostMapping("/{id}/add-meet")
    public org.springframework.http.ResponseEntity<Void> addGoogleMeet(@PathVariable String id) {
        eventService.addGoogleMeetToExistingEvent(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/add-zoom")
    public ResponseEntity<Void> addZoomToEvent(@PathVariable String id) {
        eventService.addZoomToExistingEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping("/{id}/remove-meet")
    public org.springframework.http.ResponseEntity<Void> removeGoogleMeet(@PathVariable String id) {
        eventService.removeGoogleMeetFromEvent(id);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/remove-zoom")
    public ResponseEntity<Void> removeZoomFromEvent(@PathVariable String id) {
        eventService.removeZoomFromEvent(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
