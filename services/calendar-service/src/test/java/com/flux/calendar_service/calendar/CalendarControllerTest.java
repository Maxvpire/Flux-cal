package com.flux.calendar_service.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flux.calendar_service.calendar.dto.CalendarRequest;
import com.flux.calendar_service.calendar.dto.CalendarResponse;
import com.flux.calendar_service.calendar.dto.CalendarUpdateRequest;
import com.flux.calendar_service.calendar.dto.PrimaryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private CalendarController calendarController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(calendarController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void create_Success() throws Exception {
        CalendarRequest request = new CalendarRequest("user-1", "Title", "Desc", "#fff", "UTC", true);
        when(calendarService.createCalendar(any(CalendarRequest.class))).thenReturn("cal-1");

        mockMvc.perform(post("/calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("cal-1"));
    }

    @Test
    void getAll_Success() throws Exception {
        when(calendarService.getAllCalendars()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/calendars"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getById_Success() throws Exception {
        CalendarResponse response = new CalendarResponse("cal-1", "user-1", "Title", null, null, null, false, false, null, null);
        when(calendarService.getCalendarById("cal-1")).thenReturn(response);

        mockMvc.perform(get("/calendars/id/cal-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getByUserId_Success() throws Exception {
        when(calendarService.getCalendarsByUserId("user-1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/calendars/user/id/user-1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getPrimaryCalendar_Success() throws Exception {
        CalendarResponse response = new CalendarResponse("cal-1", "user-1", "Title", null, null, null, true, false, null, null);
        when(calendarService.getPrimaryCalendar("user-1")).thenReturn(response);

        mockMvc.perform(get("/calendars/user/primary/user-1"))
                .andExpect(status().isOk());
    }

    @Test
    void makePrimary_Success() throws Exception {
        PrimaryRequest request = new PrimaryRequest("user-1");

        mockMvc.perform(put("/calendars/user/primary/cal-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(calendarService).makePrimary(eq("cal-1"), any(PrimaryRequest.class));
    }

    @Test
    void update_Success() throws Exception {
        CalendarUpdateRequest request = new CalendarUpdateRequest("Title", null, null, null);

        mockMvc.perform(put("/calendars/update/cal-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(calendarService).updateCalendar(eq("cal-1"), any(CalendarUpdateRequest.class));
    }

    @Test
    void deleteCalendar_Success() throws Exception {
        mockMvc.perform(delete("/calendars/cal-1"))
                .andExpect(status().isAccepted());

        verify(calendarService).deleteCalendar("cal-1");
    }

    @Test
    void recoverCalendar_Success() throws Exception {
        mockMvc.perform(put("/calendars/recover/cal-1"))
                .andExpect(status().isAccepted());

        verify(calendarService).recoverCalendar("cal-1");
    }
}
