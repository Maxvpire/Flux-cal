# Flux-cal API Documentation

This document provides a comprehensive list of all API endpoints available in the Flux-cal system, organized by service and controller.

## Table of Contents
- [User Management Service](#user-management-service)
  - [User Controller](#user-controller)
- [Calendar Service](#calendar-service)
  - [Calendar Controller](#calendar-controller)
  - [Event Controller](#event-controller)
  - [Conference Controller](#conference-controller)
  - [Location Controller](#location-controller)
  - [Google Auth Controller](#google-auth-controller)

---

## User Management Service

### User Controller
**Base Path:** `/users`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/users` | **Create a new user.** Expects `keycloakId` as a query parameter and `UserRequest` in the body. It creates a local user record linked to the Keycloak ID. |
| `GET` | `/users/{id}` | **Get user by ID.** Retrieves the details of a specific user using their unique internal ID. |
| `GET` | `/users` | **Get all users.** Returns a list of all registered users in the system. |
| `PUT` | `/users/{id}` | **Update user.** Updates the information of an existing user identified by their ID. |
| `DELETE` | `/users/{id}` | **Delete user.** Removes a user from the system by their ID. |

---

## Calendar Service

### Calendar Controller
**Base Path:** `/calendars`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/calendars` | **Create a new calendar.** Creates a new calendar entry for a user. |
| `GET` | `/calendars` | **Get all calendars.** Retrieves all calendars stored in the system. |
| `GET` | `/calendars/id/{id}` | **Get calendar by ID.** Retrieves details of a specific calendar. |
| `GET` | `/calendars/user/id/{id}` | **Get calendars by user ID.** Returns all calendars belonging to a specific user. |
| `GET` | `/calendars/user/primary/{id}` | **Get primary calendar.** Retrieves the primary calendar for a given user ID. |
| `GET` | `/calendars/title/{title}/user/{id}/` | **Get calendar by title.** Finds a specific calendar by its title for a given user. |
| `PUT` | `/calendars/user/primary/{id}` | **Set primary calendar.** Marks a specific calendar as the primary one for the user. |
| `PUT` | `/calendars/update/{id}` | **Update calendar.** Updates the metadata (title, description, etc.) of a calendar. |
| `PUT` | `/calendars/recover/{id}` | **Recover calendar.** Restores a previously deleted calendar. |
| `DELETE` | `/calendars/{id}` | **Delete calendar.** Deletes a calendar and its associated events. |

### Event Controller
**Base Path:** `/events`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/events/calendar/{calendarId}` | **Create event.** Creates a new event in the specified calendar. |
| `POST` | `/events/calendar/{calendarId}/add-meet` | **Create event with Google Meet.** Creates an event and automatically generates a Google Meet link. |
| `POST` | `/events/calendar/{calendarId}/add-zoom` | **Create event with Zoom.** Creates an event and automatically generates a Zoom meeting link. |
| `GET` | `/events` | **Get all events.** Retrieves all events across all calendars. |
| `GET` | `/events/{id}` | **Get event by ID.** Retrieves details of a specific event. |
| `GET` | `/events/calendar/{calendarId}` | **Get events by calendar.** Returns all events associated with a specific calendar. |
| `GET` | `/events/user/{userId}` | **Get events by user.** Returns all events for a specific user across all their calendars. |
| `PUT` | `/events/{id}` | **Update event.** Updates the details of an existing event. |
| `DELETE` | `/events/{id}` | **Delete event.** Removes an event from the calendar. |
| `PUT` | `/events/{eventId}/location/{locationId}` | **Attach location.** Links an existing location record to a specific event. |
| `PUT` | `/events/{eventId}/conference/{conferenceId}` | **Attach conference.** Links an existing conference record to a specific event. |

### Conference Controller
**Base Path:** `/conference`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/conference/{id}/add-meet` | **Add Google Meet.** Adds a Google Meet conference to an existing event identified by ID. |
| `POST` | `/conference/{id}/add-zoom` | **Add Zoom.** Adds a Zoom meeting to an existing event identified by ID. |
| `DELETE` | `/conference/{id}/remove-meet` | **Remove Google Meet.** Removes the Google Meet link from an event. |
| `DELETE` | `/conference/{id}/remove-zoom` | **Remove Zoom.** Removes the Zoom meeting link from an event. |

### Location Controller
**Base Path:** `/event/location`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/event/location/{id}` | **Create location.** Creates and attaches a new location to an event (where `{id}` is the event ID). |
| `GET` | `/event/location/maps/{id}` | **Open in Maps.** Generates a Google Maps URL for the specified location ID. |
| `GET` | `/event/location/id/{id}` | **Get location by ID.** Retrieves details of a specific location. |
| `GET` | `/event/location` | **Get all locations.** Returns a list of all location records. |
| `PUT` | `/event/location/{id}` | **Update location.** Updates the details of an existing location. |
| `DELETE` | `/event/location/{id}` | **Delete location.** Removes a location record. |

### Google Auth Controller
**Base Path:** `/auth`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/auth/google` | **Initiate Google OAuth.** Redirects the user to Google's consent page to authorize calendar access. Requires `userId`. |
| `GET` | `/auth/google/callback` | **OAuth Callback.** The endpoint Google redirects to after authorization. Handles token exchange. |
| `GET` | `/auth/status` | **Check Auth Status.** Returns whether the user is currently authenticated with Google. |
| `POST` | `/auth/logout` | **Logout.** Revokes Google tokens and clears the user session. |
| `POST` | `/auth/refresh` | **Refresh Token.** Manually triggers a refresh of the Google access token. |
