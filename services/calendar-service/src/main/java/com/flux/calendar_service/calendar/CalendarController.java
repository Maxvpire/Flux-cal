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

import java.util.List;

@RestController
@RequestMapping("/calendars")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid CalendarRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(calendarService.createCalendar(request));
    }

    @GetMapping
    public ResponseEntity<List<CalendarResponse>> getAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getAllCalendars());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<CalendarResponse> getById(@PathVariable String id){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getCalendarById(id));
    }

    @GetMapping("/user/id/{id}")
    public ResponseEntity<List<CalendarResponse>> getByUserId(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getCalendarsByUserId(id));
    }

    @GetMapping("/user/primary/{id}")
    public ResponseEntity<CalendarResponse> getPrimaryCalendar(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getPrimaryCalendar(id));
    }

    @GetMapping("/title/{title}/user/{id}/")
    public ResponseEntity<CalendarResponse> getByTitle(
            @PathVariable String id,
            @PathVariable String title)
    {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(calendarService.getByTitle(id, title));
    }

    @PutMapping("/user/primary/{id}")
    public ResponseEntity<?> makePrimary(@PathVariable String id, @RequestBody @Valid PrimaryRequest request) {
        calendarService.makePrimary(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody CalendarUpdateRequest request
    ){
        calendarService.updateCalendar(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PutMapping("/recover/{id}")
    public ResponseEntity<?> recoverCalendar(@PathVariable String id) {
        calendarService.recoverCalendar(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    };

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCalendar(@PathVariable String id) {
        calendarService.deleteCalendar(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}