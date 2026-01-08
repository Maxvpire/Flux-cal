package com.flux.calendar_service.conference;

import com.flux.calendar_service.event.EventService;
import com.flux.calendar_service.event.dto.EventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConferenceControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private ConferenceController conferenceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conferenceController).build();
    }

    @Test
    void addGoogleMeet_Success() throws Exception {
        mockMvc.perform(post("/conference/1/add-meet"))
                .andExpect(status().isOk());

        verify(eventService).addGoogleMeetToExistingEvent("1");
    }

    @Test
    void removeGoogleMeet_Success() throws Exception {
        mockMvc.perform(delete("/conference/1/remove-meet"))
                .andExpect(status().isOk());

        verify(eventService).removeGoogleMeetFromEvent("1");
    }

    @Test
    void addZoom_Success() throws Exception {
        mockMvc.perform(post("/conference/1/add-zoom"))
                .andExpect(status().isAccepted());

        verify(eventService).addZoomToExistingEvent("1");
    }

    @Test
    void removeZoom_Success() throws Exception {
        mockMvc.perform(delete("/conference/1/remove-zoom"))
                .andExpect(status().isAccepted());

        verify(eventService).removeZoomFromEvent("1");
    }
}
