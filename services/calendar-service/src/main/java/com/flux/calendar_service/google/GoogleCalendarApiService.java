package com.flux.calendar_service.google;

import com.flux.calendar_service.conference.Conference;
import com.google.api.client.util.DateTime;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import java.security.GeneralSecurityException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleCalendarApiService {
        private final GoogleAuthService googleAuthService;
        private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        @Value("${google.calendar.calendar-id:primary}")
        private String calendarId;

        @Value("${google.calendar.application-name}")
        private String applicationName;

        @Autowired
        public GoogleCalendarApiService(GoogleAuthService googleAuthService) {
                this.googleAuthService = googleAuthService;
        }

        private Calendar getCalendarClient(String userId) throws IOException {
                // Try to lazy load
                if (googleAuthService != null) {
                        Credential credential = googleAuthService.getLoadedCredential(userId);
                        if (credential != null) {
                                try {
                                        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport
                                                        .newTrustedTransport();
                                        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                                                        .setApplicationName(applicationName)
                                                        .build();
                                } catch (GeneralSecurityException | IOException e) {
                                        System.err.println(
                                                        "❌ Failed to lazy-initialize Google Calendar client for user "
                                                                        + userId + ": "
                                                                        + e.getMessage());
                                }
                        }
                }

                throw new IOException("Google Calendar client is not initialized for user " + userId
                                + ". Please authorize the application.");
        }

        // ========================================================================
        // CREATE METHODS
        // ========================================================================

        /**
         * Create a simple event in Google Calendar
         */
        public Event createEvent(String userId, String title, String description,
                        LocalDateTime startTime, LocalDateTime endTime, String type) throws IOException {

                Event event = new Event()
                                .setSummary(title)
                                .setDescription(description);

                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                event.setEnd(end);

                event.setColorId(defineColorId(type));

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);

                Event createdEvent = getCalendarClient(userId).events()
                                .insert(calendarId, event)
                                .execute();

                System.out.println("Event created for user " + userId + ": " + createdEvent.getHtmlLink());
                return createdEvent;
        }

        public Event createEventWithGoogleMeet(
                        String userId,
                        String title,
                        String description,
                        LocalDateTime startTime,
                        LocalDateTime endTime,
                        String type) throws IOException {

                Event event = new Event()
                                .setSummary(title)
                                .setDescription(description);

                // Start time
                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                // End time
                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                event.setEnd(end);

                // Color
                event.setColorId(defineColorId(type));

                // Reminders
                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());
                event.setReminders(reminders);

                // ===============================
                // GOOGLE MEET CONFERENCE
                // ===============================
                ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey()
                                .setType("hangoutsMeet");

                CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest()
                                .setRequestId(UUID.randomUUID().toString())
                                .setConferenceSolutionKey(conferenceSolutionKey);

                ConferenceData conferenceData = new ConferenceData()
                                .setCreateRequest(createConferenceRequest);

                event.setConferenceData(conferenceData);

                // Insert event WITH conferenceDataVersion
                Event createdEvent = getCalendarClient(userId)
                                .events()
                                .insert(calendarId, event)
                                .setConferenceDataVersion(1)
                                .execute();

                System.out.println("Event created with Google Meet:");
                System.out.println("Event link: " + createdEvent.getHtmlLink());

                if (createdEvent.getConferenceData() != null) {
                        System.out.println("Google Meet link: "
                                        + createdEvent.getConferenceData()
                                                        .getEntryPoints()
                                                        .get(0)
                                                        .getUri());
                }

                return createdEvent;
        }

        public Event createEventWithZoom(String userId, String title, String description,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String zoomMeetingLink, String zoomMeetingId,
                        String zoomPassword) throws IOException {

                // Step 1: Create Google Calendar Event
                Event googleEvent = new Event()
                                .setSummary(title)
                                .setDescription(buildDescriptionWithZoom(description, zoomMeetingLink, zoomPassword));

                // Set times
                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                googleEvent.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                googleEvent.setEnd(end);

                // Step 2: Add Zoom as conference data
                ConferenceData conferenceData = buildZoomConferenceData(
                                zoomMeetingLink, zoomMeetingId, zoomPassword);
                googleEvent.setConferenceData(conferenceData);

                // Step 3: Insert to Google Calendar
                Event createdEvent = getCalendarClient(userId).events()
                                .insert(calendarId, googleEvent)
                                .setConferenceDataVersion(1) // Required!
                                .setSendNotifications(true)
                                .execute();

                System.out.println("✅ Google Calendar event created with Zoom for user " + userId);
                return createdEvent;
        }

        private ConferenceData buildZoomConferenceData(String zoomLink, String zoomId, String password) {
                ConferenceSolutionKey solutionKey = new ConferenceSolutionKey()
                                .setType("addOn"); // Zoom is typically an add-on

                EntryPoint videoEntryPoint = new EntryPoint()
                                .setEntryPointType("video")
                                .setUri(zoomLink)
                                .setLabel(zoomLink);

                List<EntryPoint> entryPoints = new ArrayList<>();
                entryPoints.add(videoEntryPoint);

                return new ConferenceData()
                                .setConferenceId(zoomId)
                                .setEntryPoints(entryPoints)
                                .setNotes("Zoom Meeting Password: " + (password != null ? password : "None"));
        }

        private String buildDescriptionWithZoom(String originalDescription,
                        String zoomLink,
                        String password) {
                StringBuilder description = new StringBuilder();

                if (originalDescription != null && !originalDescription.isEmpty()) {
                        description.append(originalDescription).append("\n\n");
                }

                description.append("━━━━━━━━━━━━━━━━━━━━━━\n");
                description.append("🎥 Join Zoom Meeting\n");
                description.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");
                description.append("Meeting Link: ").append(zoomLink).append("\n");

                if (password != null && !password.isEmpty()) {
                        description.append("Password: ").append(password).append("\n");
                }

                description.append("\n━━━━━━━━━━━━━━━━━━━━━━");

                return description.toString();
        }

        public Event addZoomToExistingEvent(
                        String userId,
                        String eventId,
                        String zoomLink,
                        String zoomId,
                        String zoomPassword) throws IOException {

                // 1️⃣ Get existing event
                Event event = getCalendarClient(userId)
                                .events()
                                .get(calendarId, eventId)
                                .execute();

                // 2️⃣ Update description and conference data
                event.setDescription(buildDescriptionWithZoom(event.getDescription(), zoomLink, zoomPassword));
                event.setConferenceData(buildZoomConferenceData(zoomLink, zoomId, zoomPassword));

                // 3️⃣ Update event WITH conferenceDataVersion
                Event updatedEvent = getCalendarClient(userId)
                                .events()
                                .update(calendarId, eventId, event)
                                .setConferenceDataVersion(1)
                                .execute();

                System.out.println("Zoom added to event for user " + userId);
                return updatedEvent;
        }

        public Event removeZoomFromEvent(String userId, String eventId) throws IOException {
                Event event = getCalendarClient(userId)
                                .events()
                                .get(calendarId, eventId)
                                .execute();

                // Remove Zoom specific info from description if possible, or just clear
                // conference data
                event.setConferenceData(null);

                // We could also try to clean up the description, but it's safer to just clear
                // conference data
                // as the description might have been manually edited.

                Event updatedEvent = getCalendarClient(userId)
                                .events()
                                .update(calendarId, eventId, event)
                                .setConferenceDataVersion(1)
                                .execute();

                System.out.println("Zoom removed from event for user " + userId);
                return updatedEvent;
        }

        public Event addGoogleMeetToExistingEvent(
                        String userId,
                        String eventId) throws IOException {

                // 1️⃣ Get existing event
                Event event = getCalendarClient(userId)
                                .events()
                                .get(calendarId, eventId)
                                .execute();

                // 2️⃣ If event already has a Meet link → return it
                if (event.getConferenceData() != null) {
                        return event;
                }

                // 3️⃣ Create conference request
                ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey()
                                .setType("hangoutsMeet");

                CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest()
                                .setRequestId(UUID.randomUUID().toString())
                                .setConferenceSolutionKey(conferenceSolutionKey);

                ConferenceData conferenceData = new ConferenceData()
                                .setCreateRequest(createConferenceRequest);

                event.setConferenceData(conferenceData);

                // 4️⃣ Update event WITH conferenceDataVersion
                Event updatedEvent = getCalendarClient(userId)
                                .events()
                                .update(calendarId, eventId, event)
                                .setConferenceDataVersion(1)
                                .execute();

                System.out.println("Google Meet added to event:");
                System.out.println("Event link: " + updatedEvent.getHtmlLink());

                if (updatedEvent.getConferenceData() != null) {
                        updatedEvent.getConferenceData()
                                        .getEntryPoints()
                                        .forEach(e -> System.out.println("Meet link: " + e.getUri()));
                }

                return updatedEvent;
        }

        private String defineColorId(String type) {
                return switch (type) {
                        case "OTHERS" -> "9";
                        case "MEETING" -> "1";
                        case "APPOINTMENT" -> "3";
                        case "REMINDER" -> "4";
                        case "BIRTHDAY" -> "6";
                        case "HOLIDAY" -> "7";
                        case "TASK" -> "5";
                        case "STUDY" -> "11";
                        case "WORK" -> "2";
                        case "ROUTE" -> "8";
                        default -> null;
                };

        }

        /**
         * Create a detailed event with location, attendees, and reminders
         */
        public Event createDetailedEvent(String userId, String title, String description,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String location, List<String> attendeeEmails, String type) throws IOException {

                Event event = new Event()
                                .setSummary(title)
                                .setDescription(description)
                                .setLocation(location);

                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                event.setEnd(end);
                event.setColorId(defineColorId(type));

                // Add attendees
                if (attendeeEmails != null && !attendeeEmails.isEmpty()) {
                        List<EventAttendee> attendees = attendeeEmails.stream()
                                        .map(email -> new EventAttendee().setEmail(email))
                                        .collect(Collectors.toList());
                        event.setAttendees(attendees);
                }

                // Add reminders
                EventReminder[] reminderOverrides = new EventReminder[] {
                                new EventReminder().setMethod("email").setMinutes(24 * 60),
                                new EventReminder().setMethod("popup").setMinutes(30),
                };
                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(Arrays.asList(reminderOverrides));
                event.setReminders(reminders);

                // Send notifications to attendees
                Event createdEvent = getCalendarClient(userId).events()
                                .insert(calendarId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Detailed event created for user " + userId + ": " + createdEvent.getHtmlLink());
                return createdEvent;
        }

        /**
         * Create an all-day event
         */
        public Event createAllDayEvent(String userId, String title, String description,
                        LocalDateTime date) throws IOException {

                Event event = new Event()
                                .setSummary(title)
                                .setDescription(description);

                // For all-day events, use date instead of dateTime
                EventDateTime start = new EventDateTime()
                                .setDate(new DateTime(true,
                                                date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 0));
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDate(new DateTime(true,
                                                date.plusDays(1).atZone(ZoneId.systemDefault()).toInstant()
                                                                .toEpochMilli(),
                                                0));
                event.setEnd(end);

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);

                Event createdEvent = getCalendarClient(userId).events()
                                .insert(calendarId, event)
                                .execute();

                System.out.println("All-day event created for user " + userId + ": " + createdEvent.getHtmlLink());
                return createdEvent;
        }

        /**
         * Create a recurring event
         */
        public Event createRecurringEvent(String userId, String title, String description,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String recurrenceRule) throws IOException {

                Event event = new Event()
                                .setSummary(title)
                                .setDescription(description);

                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                event.setEnd(end);

                // Recurrence rule examples:
                // Daily: "RRULE:FREQ=DAILY;COUNT=10"
                // Weekly on Monday: "RRULE:FREQ=WEEKLY;BYDAY=MO;COUNT=10"
                // Monthly: "RRULE:FREQ=MONTHLY;COUNT=6"
                event.setRecurrence(Arrays.asList(recurrenceRule));

                Event createdEvent = getCalendarClient(userId).events()
                                .insert(calendarId, event)
                                .execute();

                System.out.println("Recurring event created for user " + userId + ": " + createdEvent.getHtmlLink());
                return createdEvent;
        }

        // ========================================================================
        // READ/GET METHODS
        // ========================================================================

        /**
         * Get a single event by ID
         */
        public Event getEventById(String userId, String eventId) throws IOException {
                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                System.out.println("Retrieved event for user " + userId + ": " + event.getSummary());
                return event;
        }

        /**
         * Get all events (paginated)
         */
        public List<Event> getAllEvents(String userId) throws IOException {
                List<Event> allEvents = new ArrayList<>();
                String pageToken = null;

                do {
                        Events events = getCalendarClient(userId).events()
                                        .list(calendarId)
                                        .setPageToken(pageToken)
                                        .execute();

                        allEvents.addAll(events.getItems());
                        pageToken = events.getNextPageToken();

                } while (pageToken != null);

                System.out.println("Retrieved " + allEvents.size() + " events for user " + userId);
                return allEvents;
        }

        /**
         * Get events within a date range
         */
        public List<Event> getEventsByDateRange(String userId, LocalDateTime startDate,
                        LocalDateTime endDate) throws IOException {

                DateTime timeMin = convertToDateTime(startDate);
                DateTime timeMax = convertToDateTime(endDate);

                Events events = getCalendarClient(userId).events()
                                .list(calendarId)
                                .setTimeMin(timeMin)
                                .setTimeMax(timeMax)
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute();

                List<Event> items = events.getItems();
                System.out.println("Retrieved " + items.size() + " events in date range for user " + userId);
                return items;
        }

        /**
         * Get upcoming events (next N events)
         */
        public List<Event> getUpcomingEvents(String userId, int maxResults) throws IOException {
                DateTime now = new DateTime(System.currentTimeMillis());

                Events events = getCalendarClient(userId).events()
                                .list(calendarId)
                                .setMaxResults(maxResults)
                                .setTimeMin(now)
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute();

                List<Event> items = events.getItems();
                System.out.println("Retrieved " + items.size() + " upcoming events for user " + userId);
                return items;
        }

        /**
         * Search events by text query
         */
        public List<Event> searchEvents(String userId, String searchQuery) throws IOException {
                Events events = getCalendarClient(userId).events()
                                .list(calendarId)
                                .setQ(searchQuery)
                                .execute();

                List<Event> items = events.getItems();
                System.out.println(
                                "Found " + items.size() + " events matching: " + searchQuery + " for user " + userId);
                return items;
        }

        /**
         * Get events updated/modified after a specific time
         */
        public List<Event> getUpdatedEvents(String userId, LocalDateTime updatedMin) throws IOException {
                DateTime updateTime = convertToDateTime(updatedMin);

                Events events = getCalendarClient(userId).events()
                                .list(calendarId)
                                .setUpdatedMin(updateTime)
                                .execute();

                List<Event> items = events.getItems();
                System.out.println("Retrieved " + items.size() + " updated events for user " + userId);
                return items;
        }

        /**
         * Get events with incremental sync (using sync token)
         */
        public EventsResult getEventsWithSyncToken(String userId, String syncToken) throws IOException {
                Calendar.Events.List request = getCalendarClient(userId).events().list(calendarId);

                if (syncToken != null) {
                        request.setSyncToken(syncToken);
                } else {
                        // Initial sync - get events from last 30 days
                        request.setTimeMin(new DateTime(System.currentTimeMillis() - 2592000000L));
                }

                List<Event> allEvents = new ArrayList<>();
                Events events;

                do {
                        events = request.execute();
                        allEvents.addAll(events.getItems());
                        request.setPageToken(events.getNextPageToken());
                } while (events.getNextPageToken() != null);

                System.out.println("Incremental sync retrieved " + allEvents.size() + " events for user " + userId);
                return new EventsResult(allEvents, events.getNextSyncToken());
        }

        // ========================================================================
        // UPDATE METHODS
        // ========================================================================

        /**
         * Update an event's basic information
         */
        public Event updateEvent(String userId, String eventId, String newTitle,
                        String newDescription) throws IOException {

                // First, retrieve the existing event
                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                // Update fields
                event.setSummary(newTitle);
                event.setDescription(newDescription);

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);

                // Update the event
                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .execute();

                System.out.println("Event updated for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        /**
         * Update event date and time
         */
        public Event updateEventDateTime(String userId, String eventId,
                        LocalDateTime newStartTime,
                        LocalDateTime newEndTime) throws IOException {

                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(newStartTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(newEndTime))
                                .setTimeZone("UTC");
                event.setEnd(end);

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);
                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Event time updated for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        /**
         * Update event location
         */
        public Event updateEventLocation(String userId, String eventId, String newLocation) throws IOException {
                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                event.setLocation(newLocation);

                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);
                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Event location updated for user " + userId + ": " + updatedEvent.getLocation());
                return updatedEvent;
        }

        /**
         * Update complete event with all details
         */
        public Event updateCompleteEvent(String userId, String eventId, String title, String description,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String location, List<String> attendeeEmails) throws IOException {

                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                event.setSummary(title);
                event.setDescription(description);
                event.setLocation(location);

                EventDateTime start = new EventDateTime()
                                .setDateTime(convertToDateTime(startTime))
                                .setTimeZone("UTC");
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                                .setDateTime(convertToDateTime(endTime))
                                .setTimeZone("UTC");
                event.setEnd(end);
                Event.Reminders reminders = new Event.Reminders()
                                .setUseDefault(false)
                                .setOverrides(new ArrayList<>());

                event.setReminders(reminders);
                if (attendeeEmails != null && !attendeeEmails.isEmpty()) {
                        List<EventAttendee> attendees = attendeeEmails.stream()
                                        .map(email -> new EventAttendee().setEmail(email))
                                        .collect(Collectors.toList());
                        event.setAttendees(attendees);
                }

                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Complete event updated for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        public Event removeConfrenceFromEvent(String userId, String eventId) throws IOException {

                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                event.setConferenceData(null);

                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .setConferenceDataVersion(1)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Complete event updated for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        /**
         * Add attendees to an existing event
         */
        public Event addAttendeesToEvent(String userId, String eventId, List<String> newAttendeeEmails)
                        throws IOException {
                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                List<EventAttendee> existingAttendees = event.getAttendees();
                if (existingAttendees == null) {
                        existingAttendees = new ArrayList<>();
                }

                for (String email : newAttendeeEmails) {
                        existingAttendees.add(new EventAttendee().setEmail(email));
                }
                event.setAttendees(existingAttendees);

                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Attendees added to event for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        /**
         * Update event color
         */
        public Event updateEventColor(String userId, String eventId, String colorId) throws IOException {
                // Color IDs: 1=Lavender, 2=Sage, 3=Grape, 4=Flamingo, 5=Banana,
                // 6=Tangerine, 7=Peacock, 8=Graphite, 9=Blueberry, 10=Basil, 11=Tomato

                Event event = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                event.setColorId(colorId);

                Event updatedEvent = getCalendarClient(userId).events()
                                .update(calendarId, eventId, event)
                                .execute();

                System.out.println("Event color updated for user " + userId + ": " + updatedEvent.getSummary());
                return updatedEvent;
        }

        // ========================================================================
        // DELETE METHODS
        // ========================================================================

        /**
         * Delete an event permanently
         */
        public void deleteEvent(String userId, String eventId) throws IOException {
                getCalendarClient(userId).events()
                                .delete(calendarId, eventId)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Event deleted for user " + userId + ": " + eventId);
        }

        /**
         * Delete event without sending notifications
         */
        public void deleteEventSilently(String userId, String eventId) throws IOException {
                getCalendarClient(userId).events()
                                .delete(calendarId, eventId)
                                .setSendNotifications(false)
                                .execute();

                System.out.println("Event deleted silently for user " + userId + ": " + eventId);
        }

        /**
         * Delete all events in a date range
         */
        public int deleteEventsByDateRange(String userId, LocalDateTime startDate,
                        LocalDateTime endDate) throws IOException {

                List<Event> events = getEventsByDateRange(userId, startDate, endDate);
                int deletedCount = 0;

                for (Event event : events) {
                        try {
                                deleteEvent(userId, event.getId());
                                deletedCount++;
                        } catch (IOException e) {
                                System.err.println("Failed to delete event: " + event.getId() + " for user " + userId);
                        }
                }

                System.out.println("Deleted " + deletedCount + " events for user " + userId);
                return deletedCount;
        }

        /**
         * Cancel a recurring event (all instances)
         */
        public void cancelRecurringEvent(String userId, String recurringEventId) throws IOException {
                Event event = getCalendarClient(userId).events()
                                .get(calendarId, recurringEventId)
                                .execute();

                event.setStatus("cancelled");

                getCalendarClient(userId).events()
                                .update(calendarId, recurringEventId, event)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Recurring event cancelled for user " + userId + ": " + recurringEventId);
        }

        /**
         * Delete a single instance of a recurring event
         */
        public void deleteRecurringEventInstance(String userId, String recurringEventId,
                        String instanceId) throws IOException {

                getCalendarClient(userId).events()
                                .delete(calendarId, instanceId)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Recurring event instance deleted for user " + userId + ": " + instanceId);
        }

        // ========================================================================
        // MOVE/COPY METHODS
        // ========================================================================

        /**
         * Move event to another calendar
         */
        public Event moveEventToCalendar(String userId, String eventId, String destinationCalendarId)
                        throws IOException {
                Event movedEvent = getCalendarClient(userId).events()
                                .move(calendarId, eventId, destinationCalendarId)
                                .setSendNotifications(true)
                                .execute();

                System.out.println("Event moved to calendar: " + destinationCalendarId + " for user " + userId);
                return movedEvent;
        }

        /**
         * Copy/duplicate an event
         */
        public Event duplicateEvent(String userId, String eventId) throws IOException {
                Event originalEvent = getCalendarClient(userId).events()
                                .get(calendarId, eventId)
                                .execute();

                // Create a new event with same details
                Event newEvent = new Event()
                                .setSummary(originalEvent.getSummary() + " (Copy)")
                                .setDescription(originalEvent.getDescription())
                                .setLocation(originalEvent.getLocation())
                                .setStart(originalEvent.getStart())
                                .setEnd(originalEvent.getEnd());

                Event duplicatedEvent = getCalendarClient(userId).events()
                                .insert(calendarId, newEvent)
                                .execute();

                System.out.println("Event duplicated for user " + userId + ": " + duplicatedEvent.getId());
                return duplicatedEvent;
        }

        // ========================================================================
        // BATCH OPERATIONS
        // ========================================================================

        /**
         * Create multiple events in batch
         */
        public List<Event> createMultipleEvents(String userId, List<EventData> eventDataList) throws IOException {
                List<Event> createdEvents = new ArrayList<>();

                for (EventData data : eventDataList) {
                        try {
                                Event event = createEvent(userId, data.title, data.description,
                                                data.startTime, data.endTime, data.type);
                                createdEvents.add(event);
                        } catch (IOException e) {
                                System.err.println("Failed to create event: " + data.title + " for user " + userId);
                        }
                }

                System.out.println("Created " + createdEvents.size() + " events in batch for user " + userId);
                return createdEvents;
        }

        /**
         * Delete multiple events by IDs
         */
        public int deleteMultipleEvents(String userId, List<String> eventIds) throws IOException {
                int deletedCount = 0;

                for (String eventId : eventIds) {
                        try {
                                deleteEvent(userId, eventId);
                                deletedCount++;
                        } catch (IOException e) {
                                System.err.println("Failed to delete event: " + eventId + " for user " + userId);
                        }
                }

                System.out.println("Deleted " + deletedCount + " events for user " + userId);
                return deletedCount;
        }

        // ========================================================================
        // CALENDAR MANAGEMENT METHODS
        // ========================================================================

        /**
         * List all calendars
         */
        public List<CalendarListEntry> listAllCalendars(String userId) throws IOException {
                CalendarList calendarList = getCalendarClient(userId).calendarList().list().execute();
                List<CalendarListEntry> items = calendarList.getItems();

                System.out.println("Found " + items.size() + " calendars for user " + userId);
                for (CalendarListEntry calendar : items) {
                        System.out.println("Calendar: " + calendar.getSummary() + " (ID: " + calendar.getId() + ")");
                }

                return items;
        }

        /**
         * Get calendar details
         */
        public com.google.api.services.calendar.model.Calendar getCalendarDetails(String userId) throws IOException {
                com.google.api.services.calendar.model.Calendar calendar = getCalendarClient(userId).calendars()
                                .get(calendarId)
                                .execute();

                System.out.println("Calendar: " + calendar.getSummary() + " for user " + userId);
                System.out.println("Timezone: " + calendar.getTimeZone());
                return calendar;
        }

        // ========================================================================
        // HELPER CLASSES AND METHODS
        // ========================================================================

        /**
         * Helper class for batch event creation
         */
        public static class EventData {
                public String title;
                public String description;
                public LocalDateTime startTime;
                public LocalDateTime endTime;
                public String type;

                public EventData(String title, String description,
                                LocalDateTime startTime, LocalDateTime endTime) {
                        this.title = title;
                        this.description = description;
                        this.startTime = startTime;
                        this.endTime = endTime;
                }
        }

        /**
         * Helper class for sync token results
         */
        public static class EventsResult {
                public List<Event> events;
                public String nextSyncToken;

                public EventsResult(List<Event> events, String nextSyncToken) {
                        this.events = events;
                        this.nextSyncToken = nextSyncToken;
                }
        }

        /**
         * Convert LocalDateTime to Google DateTime
         */
        private DateTime convertToDateTime(LocalDateTime localDateTime) {
                return new DateTime(
                                Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        }

        /**
         * Convert Google DateTime to LocalDateTime
         */
        private LocalDateTime convertToLocalDateTime(DateTime dateTime) {
                return LocalDateTime.ofInstant(
                                new Date(dateTime.getValue()).toInstant(),
                                ZoneId.systemDefault());
        }

        /**
         * Print event details (for debugging)
         */
        public void printEventDetails(Event event) {
                System.out.println("=== Event Details ===");
                System.out.println("ID: " + event.getId());
                System.out.println("Title: " + event.getSummary());
                System.out.println("Description: " + event.getDescription());
                System.out.println("Location: " + event.getLocation());
                System.out.println("Start: " + event.getStart().getDateTime());
                System.out.println("End: " + event.getEnd().getDateTime());
                System.out.println("Status: " + event.getStatus());
                System.out.println("HTML Link: " + event.getHtmlLink());
                System.out.println("====================");
        }
}
