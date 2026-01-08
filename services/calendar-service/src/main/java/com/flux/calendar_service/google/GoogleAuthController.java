package com.flux.calendar_service.google;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "Google Auth Controller", description = "Endpoints for Google OAuth2 authentication")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService authService;

    /**
     * Step 1: Initiate OAuth flow
     * GET http://localhost:8080/auth/google
     *
     * This redirects user to Google's consent page
     */
    @Operation(summary = "Authenticate with Google", description = "Initiates OAuth flow by redirecting to Google consent page")
    @ApiResponse(responseCode = "302", description = "Redirects to Google")
    @GetMapping("/google")
    public RedirectView authenticateWithGoogle(
            @RequestParam("userId") String userId,
            HttpSession session) {
        try {
            // Generate authorization URL
            String authUrl = authService.getAuthorizationUrl();

            // Store state and userId in session
            session.setAttribute("oauth_state", "random_state_" + System.currentTimeMillis());
            session.setAttribute("oauth_user_id", userId);

            System.out.println("🔐 Redirecting to Google OAuth for user: " + userId);
            System.out.println("Auth URL: " + authUrl);

            return new RedirectView(authUrl);

        } catch (Exception e) {
            System.err.println("❌ Error initiating OAuth: " + e.getMessage());
            return new RedirectView("/auth/error");
        }
    }

    /**
     * Step 2: OAuth callback endpoint
     * GET http://localhost:8080/auth/google/callback?code=xxxxx
     *
     * Google redirects here after user grants permission
     */
    @Operation(summary = "Google OAuth Callback", description = "Handles callback from Google after user grants permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Authentication failed")
    })
    @GetMapping("/google/callback")
    public Map<String, Object> handleGoogleCallback(
            @RequestParam("code") String authorizationCode,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session) {

        try {
            System.out.println("📥 Received callback from Google");
            System.out.println("Authorization code: " + authorizationCode.substring(0, 20) + "...");

            // Retrieve userId from session
            String userId = (String) session.getAttribute("oauth_user_id");
            if (userId == null) {
                throw new RuntimeException("User ID not found in session");
            }

            // Exchange authorization code for access token
            Credential credential = authService.exchangeCodeForToken(authorizationCode, userId);

            // Store credential in session
            session.setAttribute("google_credential", credential);
            session.setAttribute("user_authenticated", true);
            session.setAttribute("user_id", userId);

            System.out.println("✅ Authentication successful for user: " + userId);
            System.out.println("Access Token: " + credential.getAccessToken().substring(0, 20) + "...");
            System.out.println(
                    "Refresh Token: " + (credential.getRefreshToken() != null ? "Available" : "Not available"));

            return Map.of(
                    "status", "success",
                    "message", "Successfully authenticated with Google",
                    "userId", userId,
                    "hasRefreshToken", credential.getRefreshToken() != null,
                    "expiresIn", credential.getExpiresInSeconds());

        } catch (Exception e) {
            System.err.println("❌ Error in callback: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "status", "error",
                    "message", "Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Check authentication status
     * GET http://localhost:8080/auth/status
     */
    @Operation(summary = "Check Auth Status", description = "Checks if the current session is authenticated with Google")
    @ApiResponse(responseCode = "200", description = "Returns authentication status")
    @GetMapping("/status")
    public Map<String, Object> checkAuthStatus(HttpSession session) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("user_authenticated");
        Credential credential = (Credential) session.getAttribute("google_credential");

        if (isAuthenticated != null && isAuthenticated && credential != null) {
            return Map.of(
                    "authenticated", true,
                    "hasAccessToken", credential.getAccessToken() != null,
                    "hasRefreshToken", credential.getRefreshToken() != null,
                    "expiresIn", credential.getExpiresInSeconds() != null ? credential.getExpiresInSeconds() : 0);
        }

        return Map.of("authenticated", false);
    }

    /**
     * Logout and revoke access
     * POST http://localhost:8080/auth/logout
     */
    @Operation(summary = "Logout", description = "Logs out and optionally revokes access token")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        try {
            Credential credential = (Credential) session.getAttribute("google_credential");

            if (credential != null) {
                // Optionally revoke the token
                authService.revokeToken(credential);
            }

            // Clear session
            session.invalidate();

            System.out.println("✅ User logged out successfully");
            return Map.of("status", "success", "message", "Logged out successfully");

        } catch (Exception e) {
            System.err.println("❌ Error during logout: " + e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * Refresh access token
     * POST http://localhost:8080/auth/refresh
     */
    @Operation(summary = "Refresh Token", description = "Refreshes the access token if needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated or refresh failed")
    })
    @PostMapping("/refresh")
    public Map<String, Object> refreshToken(HttpSession session) {
        try {
            Credential credential = (Credential) session.getAttribute("google_credential");

            if (credential == null) {
                return Map.of("status", "error", "message", "Not authenticated");
            }

            // Refresh the token
            boolean refreshed = authService.refreshAccessToken(credential);

            if (refreshed) {
                // Update session with new credential
                session.setAttribute("google_credential", credential);

                return Map.of(
                        "status", "success",
                        "message", "Token refreshed successfully",
                        "expiresIn", credential.getExpiresInSeconds());
            } else {
                return Map.of("status", "error", "message", "Failed to refresh token");
            }

        } catch (Exception e) {
            System.err.println("❌ Error refreshing token: " + e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
