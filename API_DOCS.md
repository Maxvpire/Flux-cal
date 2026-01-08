# Flux Calendar API Documentation

## Overview
This document provides reference documentation for the REST APIs exposed by the Flux Calendar microservices.

## Services
- **Calendar Service**: Manages calendars, events, tasks, attachments, and 3rd party integrations (Google/Zoom).
- **User Management Service**: Manages user identities and profiles.

---

## 1. Calendar Service

### 1.1 Attachments
Endpoints for managing file attachments on events.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/attachments/event/{eventId}` | Upload attachment | Uploads a file attachment for a specific event. consuming `multipart/form-data`. |
| `GET` | `/attachments/{attachmentId}/file` | Download file | Downloads the file associated with an attachment ID. |
| `DELETE` | `/attachments/{attachmentId}` | Delete attachment | Deletes an attachment by its ID. |

### 1.2 Calendars
Endpoints for managing calendar definitions.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/calendars` | Create Calendar | Creates a new calendar. Payload: `CalendarRequest`. |
| `GET` | `/calendars` | Get All | Retrieves all calendars. |
| `GET` | `/calendars/id/{id}` | Get by ID | Retrieves a specific calendar by ID. |
| `GET` | `/calendars/user/id/{id}` | Get by User | Retrieves all calendars for a specific user ID. |
| `GET` | `/calendars/user/primary/{id}` | Get Primary | Retrieves the primary calendar for a user. |
| `GET` | `/calendars/title/{title}/user/{id}/` | Get by Title | Retrieves a calendar by title and user ID. |
| `PUT` | `/calendars/user/primary/{id}` | Make Primary | Sets a calendar as primary for the user. |
| `PUT` | `/calendars/update/{id}` | Update Calendar | Updates an existing calendar. Payload: `CalendarUpdateRequest`. |
| `PUT` | `/calendars/recover/{id}` | Recover Calendar | Recovers a deleted calendar. |
| `DELETE` | `/calendars/{id}` | Delete Calendar | Deletes a calendar by ID. |

### 1.3 Conferences
Endpoints for managing video conferences (Google Meet, Zoom).

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/conference/{id}/add-meet` | Add Google Meet | Adds a Google Meet link to an existing event. |
| `POST` | `/conference/{id}/add-zoom` | Add Zoom Meeting | Adds a Zoom meeting link to an existing event. |
| `DELETE` | `/conference/{id}/remove-meet` | Remove Google Meet | Removes a Google Meet link from an event. |
| `DELETE` | `/conference/{id}/remove-zoom` | Remove Zoom Meeting | Removes a Zoom meeting from an event. |

### 1.4 Events
Endpoints for managing calendar events.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/events/calendar/{calendarId}` | Create Event | Creates a new event in the specified calendar. Payload: `EventRequest`. |
| `POST` | `/events/calendar/{calendarId}/add-meet` | Create w/ Meet | Creates a new event with a Google Meet link. |
| `POST` | `/events/calendar/{calendarId}/add-zoom` | Create w/ Zoom | Creates a new event with a Zoom meeting link. |
| `GET` | `/events` | Get All | Retrieves all events. |
| `GET` | `/events/{id}` | Get by ID | Retrieves a specific event by ID. |
| `GET` | `/events/calendar/{calendarId}` | Get by Calendar | Retrieves all events for a specific calendar. |
| `GET` | `/events/user/{userId}` | Get by User | Retrieves all events for a specific user. |
| `PUT` | `/events/{id}` | Update Event | Updates an existing event. Payload: `EventUpdateRequest`. |
| `PUT` | `/events/{eventId}/location/{locationId}` | Attach Location | Attaches a location to an event. |
| `PUT` | `/events/{eventId}/conference/{conferenceId}` | Attach Conference | Attaches a conference to an event. |
| `DELETE` | `/events/{id}` | Delete Event | Deletes an event by ID. |

### 1.5 Google Auth
Endpoints for Google OAuth2 authentication (if enabled).

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/auth/google` | Authenticate | Initiates OAuth flow by redirecting to Google consent page. Params: `userId`. |
| `GET` | `/auth/google/callback` | Callback | Handles callback from Google code exchange. |
| `GET` | `/auth/status` | Check Status | Checks if the current session is authenticated. |
| `POST` | `/auth/logout` | Logout | Logs out and optionally revokes access token. |
| `POST` | `/auth/refresh` | Refresh Token | Refreshes the access token if needed. |

### 1.6 Locations
Endpoints for managing event locations.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/event/location/{id}` | Create Location | Creates a location for a specific event ID. Payload: `LocationRequest`. |
| `GET` | `/event/location/maps/{id}` | Open in Maps | Generates a maps link (e.g., Google Maps) for the location. |
| `GET` | `/event/location/id/{id}` | Get by ID | Retrieves a specific location by ID. |
| `GET` | `/event/location` | Get All | Retrieves a list of all locations. |
| `PUT` | `/event/location/{id}` | Update Location | Updates an existing location. Payload: `UpdateLocation`. |
| `DELETE` | `/event/location/{id}` | Delete Location | Deletes a location by ID. |

### 1.7 Tasks
Endpoints for managing tasks associated with events.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/tasks/event/{eventId}` | Create Task | Creates a new task for an event. Payload: `String` (name). |
| `GET` | `/tasks/event/{eventId}` | Get by Event | Retrieves all tasks for a specific event. |
| `GET` | `/tasks` | Get All | Retrieves all tasks. |
| `GET` | `/tasks/{id}` | Get by ID | Retrieves a specific task by ID. |
| `PATCH` | `/tasks/{id}/toggle` | Toggle Status | Toggles the completion status of a task. |
| `PUT` | `/tasks/{id}` | Update Task | Updates an existing task. Payload: `TaskRequest`. |
| `DELETE` | `/tasks/{id}` | Delete Task | Deletes a task by ID. |

---

## 2. User Management Service

### 2.1 Users
Endpoints for managing user accounts.

| Method | Path | Summary | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/users` | Create User | Creates a new user. Params: `keycloakId`. Payload: `UserRequest`. |
| `GET` | `/users/{id}` | Get by ID | Retrieves a user by their unique ID. |
| `GET` | `/users` | Get All | Retrieves all registered users. |
| `PUT` | `/users/{id}` | Update User | Updates an existing user. Payload: `UserRequest`. |
| `DELETE` | `/users/{id}` | Delete User | Deletes a user by their ID. |
