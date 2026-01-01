package com.flux.calendar_service.location;

import com.flux.calendar_service.calendar.Calendar;
import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.google.GoogleCalendarApiService;
import com.flux.calendar_service.location.dto.LocationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private GoogleCalendarApiService googleCalendarApiService;

    @InjectMocks
    private LocationService locationService;

    private Event event;
    private LocationRequest locationRequest;

    @BeforeEach
    void setUp() {
        Calendar calendar = Calendar.builder()
                .userId("user-123")
                .build();

        event = Event.builder()
                .id("event-123")
                .googleCalendarId("google-event-123")
                .calendar(calendar)
                .build();

        locationRequest = new LocationRequest(
                "Office",
                "123 Street",
                "New York",
                "USA",
                "Tech Tower",
                5,
                "Room 501",
                40.7128,
                -74.0060,
                "place-123"
        );
    }

    @Test
    void addLocation_Success() throws IOException {
        // Arrange
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));
        
        Location savedLocation = Location.builder()
                .id("loc-123")
                .placeName(locationRequest.placeName())
                .streetAddress(locationRequest.streetAddress())
                .city(locationRequest.city())
                .country(locationRequest.country())
                .build();
        
        when(locationRepository.save(any(Location.class))).thenReturn(savedLocation);

        // Act
        String resultId = locationService.addLocation("event-123", locationRequest);

        // Assert
        assertEquals("loc-123", resultId);
        assertNotNull(event.getLocation());
        assertEquals("loc-123", event.getLocation().getId());
        
        verify(eventRepository).save(event);
        verify(googleCalendarApiService).updateEventLocation(
                eq("user-123"),
                eq("google-event-123"),
                contains("Office, 123 Street, New York, USA")
        );
    }

    @Test
    void addLocation_NoGoogleSync_WhenGoogleIdMissing() throws IOException {
        // Arrange
        event.setGoogleCalendarId(null);
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));
        
        Location savedLocation = Location.builder()
                .id("loc-123")
                .build();
        
        when(locationRepository.save(any(Location.class))).thenReturn(savedLocation);

        // Act
        locationService.addLocation("event-123", locationRequest);

        // Assert
        verify(googleCalendarApiService, never()).updateEventLocation(any(), any(), any());
    }
}
