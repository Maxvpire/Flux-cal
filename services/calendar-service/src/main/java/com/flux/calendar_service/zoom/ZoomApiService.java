package com.flux.calendar_service.zoom;

import com.flux.calendar_service.exceptions.ZoomCredentionalsNotFullyConfiguredException;
import com.flux.calendar_service.exceptions.ZoomMeetingFailedException;
import com.flux.calendar_service.zoom.dto.ZoomMeetingRequest;
import com.flux.calendar_service.zoom.dto.ZoomMeetingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoomApiService {

    private final RestTemplate restTemplate;

    @Value("${zoom.api.account-id}")
    private String accountId;

    @Value("${zoom.api.client-id}")
    private String clientId;

    @Value("${zoom.api.client-secret}")
    private String clientSecret;

    private static final String ZOOM_AUTH_URL = "https://zoom.us/oauth/token";
    private static final String ZOOM_API_BASE_URL = "https://api.zoom.us/v2";

    public ZoomMeetingResponse createMeeting(ZoomMeetingRequest request) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<ZoomMeetingRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ZoomMeetingResponse> response = restTemplate.exchange(
                    ZOOM_API_BASE_URL + "/users/me/meetings",
                    HttpMethod.POST,
                    entity,
                    ZoomMeetingResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to create Zoom meeting", e);
            throw new ZoomMeetingFailedException("Failed to create Zoom meeting" + e.getMessage());
        }
    }

    public void deleteMeeting(String meetingId) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    ZOOM_API_BASE_URL + "/meetings/" + meetingId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class);
        } catch (Exception e) {
            log.error("Failed to delete Zoom meeting: {}", meetingId, e);
            throw new ZoomMeetingFailedException("Failed to delete Zoom meeting: " + meetingId + e.getMessage());
        }
    }

    private String getAccessToken() {
        String trimmedClientId = clientId != null ? clientId.trim() : null;
        String trimmedClientSecret = clientSecret != null ? clientSecret.trim() : null;
        String trimmedAccountId = accountId != null ? accountId.trim() : null;

        // Check if environment variables were actually expanded
        if (isEnvVarPlaceholder(trimmedClientId) || isEnvVarPlaceholder(trimmedClientSecret)
                || isEnvVarPlaceholder(trimmedAccountId)) {
            log.error(
                    "Zoom environment variables are not expanded! Check your configuration. ClientID: {}, ClientSecret: {}, AccountID: {}",
                    trimmedClientId, trimmedClientSecret, trimmedAccountId);
                    // Zoom environment variables are not expanded
            throw new ZoomMeetingFailedException("Something went wrong! try again!");
        }

        log.debug("Fetching Zoom access token. Client ID: {}, Account ID: {}",
                maskString(trimmedClientId), maskString(trimmedAccountId));

        if (trimmedClientId == null || trimmedClientSecret == null || trimmedAccountId == null) {
            log.error("Zoom credentials are not fully configured. ClientID: {}, ClientSecret: {}, AccountID: {}",
                    trimmedClientId != null ? "SET" : "MISSING",
                    trimmedClientSecret != null ? "SET" : "MISSING",
                    trimmedAccountId != null ? "SET" : "MISSING");
            throw new ZoomCredentionalsNotFullyConfiguredException("Zoom credentials are not fully configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(trimmedClientId, trimmedClientSecret);

        // Zoom documentation for Server-to-Server OAuth explicitly shows params as
        // query strings
        String url = String.format("%s?grant_type=account_credentials&account_id=%s",
                ZOOM_AUTH_URL, trimmedAccountId);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    url,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            } else {
                log.error("Zoom auth response did not contain access_token. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new ZoomMeetingFailedException("Failed to get Zoom access token: No token in response");
            }
        } catch (Exception e) {
            log.error("Failed to get Zoom access token. URL: {}", url, e);
            throw new ZoomMeetingFailedException("Failed to get Zoom access token" + e.getMessage());
        }
    }

    private boolean isEnvVarPlaceholder(String value) {
        return value != null && value.startsWith("${") && value.endsWith("}");
    }

    private String maskString(String str) {
        if (str == null || str.length() < 4)
            return "****";
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }
}
