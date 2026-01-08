package com.flux.calendar_service.zoom;

import com.flux.calendar_service.zoom.dto.ZoomMeetingRequest;
import com.flux.calendar_service.zoom.dto.ZoomMeetingResponse;
import com.flux.calendar_service.exceptions.ZoomMeetingFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoomApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ZoomApiService zoomApiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zoomApiService, "accountId", "acc-id");
        ReflectionTestUtils.setField(zoomApiService, "clientId", "cli-id");
        ReflectionTestUtils.setField(zoomApiService, "clientSecret", "cli-sec");
    }
    
    // In ZoomApiService, getAccessToken() is private and called internally.
    // We need to mock the token call FIRST, then the actual API call.
    // getAccessToken() makes a POST to https://zoom.us/oauth/token

    @Test
    void createMeeting_Success() {
        // Mock Access Token Call
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", "test-token");
        ResponseEntity<Map> tokenResponse = new ResponseEntity<>(tokenMap, HttpStatus.OK);
        
        when(restTemplate.postForEntity(contains("oauth/token"), any(), eq(Map.class)))
                .thenReturn(tokenResponse);

        // Mock Create Meeting Call
        ZoomMeetingResponse meetingResponse = new ZoomMeetingResponse(); // assume empty or minimal object
        // we can't set fields on ZoomMeetingResponse easily if it's a record or has no setters? 
        // Let's assume default constructor works or check DTO.
        // It's likely a class. 
        
        ResponseEntity<ZoomMeetingResponse> apiResponse = new ResponseEntity<>(meetingResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(
                contains("/users/me/meetings"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ZoomMeetingResponse.class)))
                .thenReturn(apiResponse);

        // Act
        ZoomMeetingRequest request = new ZoomMeetingRequest(); // assuming default constructor
        ZoomMeetingResponse result = zoomApiService.createMeeting(request);

        // Assert
        assertNotNull(result);
    }

    @Test
    void deleteMeeting_Success() {
        // Mock Access Token Call
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", "test-token");
        ResponseEntity<Map> tokenResponse = new ResponseEntity<>(tokenMap, HttpStatus.OK);

        when(restTemplate.postForEntity(contains("oauth/token"), any(), eq(Map.class)))
                .thenReturn(tokenResponse);

        // Mock Delete Call
        when(restTemplate.exchange(
                contains("/meetings/123"),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // Act
        zoomApiService.deleteMeeting("123");

        // Assert
        verify(restTemplate).postForEntity(contains("oauth/token"), any(), eq(Map.class));
        verify(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
    }
}
