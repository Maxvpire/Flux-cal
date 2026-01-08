package com.flux.calendar_service.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flux.calendar_service.location.dto.LocationRequest;
import com.flux.calendar_service.location.dto.LocationResponse;
import com.flux.calendar_service.location.dto.OpenInMapResponse;
import com.flux.calendar_service.location.dto.UpdateLocation;
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
class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController locationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createLocation_Success() throws Exception {
        LocationRequest request = new LocationRequest("Place", "St", "City", "Co", "Bldg", 1, "Rm", 0.0, 0.0, "pid");
        when(locationService.addLocation(eq("evt-1"), any(LocationRequest.class))).thenReturn("loc-1");

        mockMvc.perform(post("/event/location/evt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("loc-1"));
    }

    @Test
    void findById_Success() throws Exception {
        LocationResponse response = new LocationResponse("loc-1", "Place", "St", "City", "Co", "Bldg", 1, "Rm", 0.0, 0.0, "pid");
        when(locationService.findById("loc-1")).thenReturn(response);

        mockMvc.perform(get("/event/location/id/loc-1"))
                .andExpect(status().isOk());
    }

    @Test
    void openInMaps_Success() throws Exception {
        OpenInMapResponse response = new OpenInMapResponse("google", "yandex", "ymaps");
        when(locationService.openInMaps("loc-1")).thenReturn(response);

        mockMvc.perform(get("/event/location/maps/loc-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_Success() throws Exception {
        when(locationService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/event/location"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void update_Success() throws Exception {
        UpdateLocation request = new UpdateLocation("New Place", null, null, null, null, 0, null, 0.0, 0.0, null);

        mockMvc.perform(put("/event/location/loc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(locationService).updateLocation(eq("loc-1"), any(UpdateLocation.class));
    }

    @Test
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/event/location/loc-1"))
                .andExpect(status().isAccepted());

        verify(locationService).deleteLocation("loc-1");
    }
}
