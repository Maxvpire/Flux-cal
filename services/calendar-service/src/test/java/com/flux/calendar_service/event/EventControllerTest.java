package com.flux.calendar_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flux.calendar_service.event.dto.EventRequest;
import com.flux.calendar_service.event.dto.EventResponse;
import com.flux.calendar_service.event.dto.EventUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createEvent_Success() throws Exception {
        EventRequest request = new EventRequest(
                "Title", "Desc", "#fff", EventType.MEETING, LocalDate.now(),
                LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                false, null, null, null
        );

        when(eventService.createEvent(eq("cal-1"), any(EventRequest.class))).thenReturn("evt-1");

        mockMvc.perform(post("/events/calendar/cal-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("evt-1"));
    }

    @Test
    void createEventWithMeet_Success() throws Exception {
        EventRequest request = new EventRequest(
                "Title", "Desc", "#fff", EventType.MEETING, LocalDate.now(),
                LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                false, null, null, null
        );

        when(eventService.createEventWithGoogleMeet(eq("cal-1"), any(EventRequest.class))).thenReturn("evt-1");

        mockMvc.perform(post("/events/calendar/cal-1/add-meet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("evt-1"));
    }

    @Test
    void getAllEvents_Success() throws Exception {
        when(eventService.getAllEvents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getEventById_Success() throws Exception {
        EventResponse response = new EventResponse("evt-1", null, "Title", null, null, null, null, null, null, null, false, null, null, null, null, null, null);
        when(eventService.getEventById("evt-1")).thenReturn(response);

        mockMvc.perform(get("/events/evt-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getEventsByCalendar_Success() throws Exception {
        when(eventService.getEventsByCalendarId("cal-1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/events/calendar/cal-1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void updateEvent_Success() throws Exception {
        EventUpdateRequest request = new EventUpdateRequest("Updated", null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/events/evt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(eventService).updateEvent(eq("evt-1"), any(EventUpdateRequest.class));
    }

    @Test
    void deleteEvent_Success() throws Exception {
        mockMvc.perform(delete("/events/evt-1"))
                .andExpect(status().isAccepted());

        verify(eventService).deleteEvent("evt-1");
    }

    @Test
    void attachLocation_Success() throws Exception {
        mockMvc.perform(put("/events/evt-1/location/loc-1"))
                .andExpect(status().isAccepted());

        verify(eventService).attachLocation("evt-1", "loc-1");
    }
}
