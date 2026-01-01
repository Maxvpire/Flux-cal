package com.flux.calendar_service.google;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleAuthService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Value("${google.calendar.client-id:#{null}}")
    private String clientId;

    @Value("${google.calendar.client-secret:#{null}}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri:http://localhost:8080/auth/google/callback}")
    private String redirectUri;

    private NetHttpTransport httpTransport;
    private GoogleAuthorizationCodeFlow flow;

    @PostConstruct
    public void initialize() throws Exception {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        initializeFlowFromConfig();

        System.out.println("✅ Google OAuth Service initialized");
        System.out.println("Redirect URI: " + redirectUri);
    }

    /**
     * Initialize flow using client ID and secret from config
     */
    private void initializeFlowFromConfig() throws IOException {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(clientId)
                .setClientSecret(clientSecret);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(details);

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new java.io.File("tokens"));

        this.flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .setApprovalPrompt("force") // Force to get refresh token
                .build();

        System.out.println("OAuth flow initialized from configuration");
    }

    /**
     * Generate authorization URL for user to grant access
     */
    public String getAuthorizationUrl() {
        String authUrl = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setAccessType("offline")
                .setApprovalPrompt("force") // Force consent screen to get refresh token
                .build();

        return authUrl;
    }

    /**
     * Exchange authorization code for access token and refresh token
     */
    public Credential exchangeCodeForToken(String authorizationCode, String userId) throws IOException {
        TokenResponse response = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(redirectUri)
                .execute();

        // Create credential from token response
        Credential credential = flow.createAndStoreCredential(response, userId);

        System.out.println("Token exchange successful for user: " + userId);
        System.out.println("Access Token: " + credential.getAccessToken().substring(0, 10) + "...");
        System.out.println("Refresh Token: " + (credential.getRefreshToken() != null ? "Available" : "Not available"));
        System.out.println("Expires in: " + credential.getExpiresInSeconds() + " seconds");

        return credential;
    }

    /**
     * Refresh the access token using refresh token
     */
    public boolean refreshAccessToken(Credential credential) throws IOException {
        if (credential.getRefreshToken() == null) {
            System.err.println("❌ No refresh token available");
            return false;
        }

        boolean refreshed = credential.refreshToken();

        if (refreshed) {
            System.out.println("✅ Access token refreshed");
            System.out.println("New expires in: " + credential.getExpiresInSeconds() + " seconds");
        }

        return refreshed;
    }

    /**
     * Revoke the access token (logout)
     */
    public void revokeToken(Credential credential) throws IOException {
        if (credential.getAccessToken() != null) {
            // Revoke token via Google API
            httpTransport.createRequestFactory()
                    .buildGetRequest(new com.google.api.client.http.GenericUrl(
                            "https://oauth2.googleapis.com/revoke?token=" + credential.getAccessToken()))
                    .execute();

            System.out.println("✅ Token revoked successfully");
        }
    }

    /**
     * Check if credential is valid and not expired
     */
    public boolean isCredentialValid(Credential credential) {
        if (credential == null || credential.getAccessToken() == null) {
            return false;
        }

        Long expiresIn = credential.getExpiresInSeconds();
        return expiresIn != null && expiresIn > 300; // Valid if more than 5 minutes left
    }

    /**
     * Get the authorization code flow
     */
    public GoogleAuthorizationCodeFlow getFlow() {
        return flow;
    }

    /**
     * Get the loaded credential if available
     */
    public Credential getLoadedCredential(String userId) throws IOException {
        if (flow == null || userId == null) {
            return null;
        }
        return flow.loadCredential(userId);
    }
}
