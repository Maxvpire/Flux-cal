package com.flux.calendar_service.event;

import com.flux.calendar_service.calendar.Calendar;
import com.flux.calendar_service.calendar.CalendarRepository;
import com.flux.calendar_service.conference.Conference;
import com.flux.calendar_service.conference.ConferenceMapper;
import com.flux.calendar_service.conference.ConferenceRepository;
import com.flux.calendar_service.event.dto.EventRequest;
import com.flux.calendar_service.event.dto.EventResponse;
import com.flux.calendar_service.event.dto.EventUpdateRequest;
import com.flux.calendar_service.exceptions.AddGoogleMeetFailedException;
import com.flux.calendar_service.exceptions.ConflictException;
import com.flux.calendar_service.exceptions.GoogleCalendarDisabledException;
import com.flux.calendar_service.exceptions.GoogleCalendarSyncFailedException;
import com.flux.calendar_service.exceptions.IncorrectTimeException;
import com.flux.calendar_service.exceptions.MustNotBeEmptyException;
import com.flux.calendar_service.exceptions.RemoveGoogleMeetFailedException;
import com.flux.calendar_service.google.GoogleCalendarApiService;
import com.flux.calendar_service.location.Location;
import com.flux.calendar_service.location.LocationMapper;
import com.flux.calendar_service.location.LocationRepository;
import com.flux.calendar_service.zoom.ZoomApiService;
import com.flux.calendar_service.zoom.dto.ZoomMeetingRequest;
import com.flux.calendar_service.zoom.dto.ZoomMeetingResponse;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CalendarRepository calendarRepository;
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final ConferenceRepository conferenceRepository;
    private final ConferenceMapper conferenceMapper;
    private final ZoomApiService zoomApiService;

    @Autowired(required = false)
    private GoogleCalendarApiService googleCalendarApiService;

    @Transactional
    public String createEvent(String calendarId, EventRequest request) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new MustNotBeEmptyException("Calendar ID cannot be empty");
        }

        // Validate calendar exists
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        // Validate time if not all-day event
        if (!request.allDay() && request.startTime() != null && request.endTime() != null) {
            if (request.endTime().isBefore(request.startTime())) {
                throw new IncorrectTimeException("End time cannot be before start time");
            }
        }

        // Create event
        Event event = eventMapper.toEvent(request, calendar);

        // Save event first to get the ID
        if (event.getTasks() != null) {
            event.getTasks().forEach(task -> task.setEvent(event));
        }
        if (event.getAttachments() != null) {
            event.getAttachments().forEach(attachment -> attachment.setEvent(event));
        }
        Event savedEvent = eventRepository.save(event);

        // Sync with Google Calendar if enabled
        if (googleCalendarApiService != null) {
            try {
                com.google.api.services.calendar.model.Event googleEvent;
                String userId = calendar.getUserId();

                if (savedEvent.isAllDay()) {
                    // Create all-day event
                    googleEvent = googleCalendarApiService.createAllDayEvent(
                            userId,
                            savedEvent.getTitle(),
                            savedEvent.getDescription(),
                            savedEvent.getStartTime());
                } else {
                    // Create simple event
                    googleEvent = googleCalendarApiService.createEvent(
                            userId,
                            savedEvent.getTitle(),
                            savedEvent.getDescription(),
                            savedEvent.getStartTime(),
                            savedEvent.getEndTime(),
                            savedEvent.getType().toString());
                }

                // Update event with Google Calendar ID
                savedEvent.setGoogleCalendarId(googleEvent.getId());
                savedEvent.setSyncStatus(SyncStatus.SYNCED);
                eventRepository.save(savedEvent);

                log.info("Event created and synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        savedEvent.getId(), googleEvent.getId());
            } catch (IOException e) {
                savedEvent.setSyncStatus(SyncStatus.PENDING);
                eventRepository.save(savedEvent);
                throw new GoogleCalendarSyncFailedException("Failed to sync event to Google Calendar. Event ID: {}" + savedEvent.getId() + e.getMessage());
            }
        } else {
            throw new GoogleCalendarDisabledException("Google Calendar integration is disabled. Event created locally only.");
        }

        return savedEvent.getId();
    }

    @Transactional
    public String createEventWithGoogleMeet(String calendarId, EventRequest request) {

        if (calendarId == null || calendarId.isBlank()) {
            throw new MustNotBeEmptyException("Calendar ID cannot be empty");
        }

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        if (!request.allDay()
                && request.startTime() != null
                && request.endTime() != null
                && request.endTime().isBefore(request.startTime())) {
            throw new IncorrectTimeException("End time cannot be before start time");
        }

        Event event = eventMapper.toEvent(request, calendar);
        if (event.getTasks() != null) {
            event.getTasks().forEach(task -> task.setEvent(event));
        }
        if (event.getAttachments() != null) {
            event.getAttachments().forEach(attachment -> attachment.setEvent(event));
        }
        Event savedEvent = eventRepository.save(event);

        // 4️⃣ Google sync (optional)
        if (googleCalendarApiService == null) {
            throw new GoogleCalendarDisabledException("Google Calendar integration disabled. Event created locally only.");
        }

        try {
            String userId = calendar.getUserId();
            com.google.api.services.calendar.model.Event googleEvent;

            if (savedEvent.isAllDay()) {
                // ✔ All-day → NO Meet
                googleEvent = googleCalendarApiService.createAllDayEvent(
                        userId,
                        savedEvent.getTitle(),
                        savedEvent.getDescription(),
                        savedEvent.getStartTime());
            } else {
                // ✔ Timed → WITH Meet
                googleEvent = googleCalendarApiService.createEventWithGoogleMeet(
                        userId,
                        savedEvent.getTitle(),
                        savedEvent.getDescription(),
                        savedEvent.getStartTime(),
                        savedEvent.getEndTime(),
                        savedEvent.getType().toString());
            }

            // 5️⃣ Extract Meet details (timed events only)
            String googleConferenceId = null;
            String meetLink = null;
            String meetingCode = null;
            String phoneNumber = null;
            String pin = null;

            if (!savedEvent.isAllDay() && googleEvent.getConferenceData() != null) {
                com.google.api.services.calendar.model.ConferenceData confData = googleEvent.getConferenceData();
                googleConferenceId = confData.getConferenceId();

                if (confData.getEntryPoints() != null) {
                    for (com.google.api.services.calendar.model.EntryPoint entry : confData.getEntryPoints()) {
                        if ("video".equals(entry.getEntryPointType())) {
                            meetLink = entry.getUri();
                            if (meetLink != null && meetLink.contains("/")) {
                                meetingCode = meetLink.substring(meetLink.lastIndexOf("/") + 1);
                            }
                        } else if ("phone".equals(entry.getEntryPointType())) {
                            phoneNumber = entry.getUri();
                            pin = entry.getPin();
                        }
                    }
                }
            }

            Conference conference = Conference.builder()
                    .type(Conference.ConferenceType.GOOGLE_MEET)
                    .googleConferenceId(googleConferenceId)
                    .meetLink(meetLink)
                    .meetingCode(meetingCode)
                    .phoneNumber(phoneNumber)
                    .pin(pin)
                    .conferenceLink(meetLink)
                    .syncStatus(Conference.SyncStatus.SYNCED)
                    .lastSynced(LocalDateTime.now())
                    .event(savedEvent)
                    .build();

            // 6️⃣ Update and save local event (cascade will save conference)
            savedEvent.setGoogleCalendarId(googleEvent.getId());
            savedEvent.setSyncStatus(SyncStatus.SYNCED);
            savedEvent.setConference(conference);

            eventRepository.save(savedEvent);

            log.info(
                    "Event synced. Local ID: {}, Google ID: {}, Meet: {}",
                    savedEvent.getId(),
                    googleEvent.getId(),
                    meetLink);

        } catch (IOException e) {
            savedEvent.setSyncStatus(SyncStatus.PENDING);
            eventRepository.save(savedEvent);
            throw new GoogleCalendarSyncFailedException("Failed to sync event to Google Calendar. Event ID: " + savedEvent.getId() + e.getMessage());
        }

        return savedEvent.getId();
    }

    @Transactional
    public void addGoogleMeetToExistingEvent(String id) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        if (googleCalendarApiService == null) {
            throw new GoogleCalendarDisabledException("Google Calendar integration is disabled");
        }

        try {
            String userId = event.getCalendar().getUserId();
            com.google.api.services.calendar.model.Event googleEvent = googleCalendarApiService
                    .addGoogleMeetToExistingEvent(
                            userId,
                            getGoogleEventId(event));

            // Extract Meet details
            String googleConferenceId = null;
            String meetLink = null;
            String meetingCode = null;
            String phoneNumber = null;
            String pin = null;

            if (googleEvent.getConferenceData() != null) {
                com.google.api.services.calendar.model.ConferenceData confData = googleEvent.getConferenceData();
                googleConferenceId = confData.getConferenceId();

                if (confData.getEntryPoints() != null) {
                    for (com.google.api.services.calendar.model.EntryPoint entry : confData.getEntryPoints()) {
                        if ("video".equals(entry.getEntryPointType())) {
                            meetLink = entry.getUri();
                            if (meetLink != null && meetLink.contains("/")) {
                                meetingCode = meetLink.substring(meetLink.lastIndexOf("/") + 1);
                            }
                        } else if ("phone".equals(entry.getEntryPointType())) {
                            phoneNumber = entry.getUri();
                            pin = entry.getPin();
                        }
                    }
                }
            }

            Conference conference = Conference.builder()
                    .type(Conference.ConferenceType.GOOGLE_MEET)
                    .googleConferenceId(googleConferenceId)
                    .meetLink(meetLink)
                    .meetingCode(meetingCode)
                    .phoneNumber(phoneNumber)
                    .pin(pin)
                    .conferenceLink(meetLink)
                    .syncStatus(Conference.SyncStatus.SYNCED)
                    .lastSynced(LocalDateTime.now())
                    .event(event)
                    .build();

            event.setConference(conference);
            event.setSyncStatus(SyncStatus.SYNCED);
            eventRepository.save(event);

            log.info("Google Meet added to event. Local ID: {}, Google ID: {}, Meet: {}",
                    event.getId(), googleEvent.getId(), meetLink);

        } catch (IOException e) {
            log.error("Failed to add Google Meet to event. Event ID: {}", event.getId(), e);
            throw new AddGoogleMeetFailedException("Failed to add Google Meet" + e.getMessage());
        }
    }

    @Transactional
    public void removeGoogleMeetFromEvent(String id) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        if (event.getConference() == null) {
            return;
        }

        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                com.google.api.services.calendar.model.Event googleEvent = googleCalendarApiService.getEventById(userId,
                        getGoogleEventId(event));
                googleEvent.setConferenceData(null);

                // Update Google
                googleCalendarApiService.removeConfrenceFromEvent(
                        userId,
                        getGoogleEventId(event));
            } catch (IOException e) {
                throw new RemoveGoogleMeetFailedException("Failed to remove Google Meet from Google Calendar. Event ID: {}" + event.getId() + e.getMessage());
            }
        }

        Conference conference = event.getConference();
        event.setConference(null);
        eventRepository.save(event);
        conferenceRepository.delete(conference);

        log.info("Google Meet removed from event. Local ID: {}", id);
    }

    @Transactional
    public String createEventWithNewZoomMeeting(String calendarId, EventRequest request) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new MustNotBeEmptyException("Calendar ID cannot be empty");
        }

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        // 1. Create Zoom Meeting
        ZoomMeetingRequest zoomRequest = ZoomMeetingRequest.builder()
                .topic(request.title())
                .type(2) // Scheduled
                .start_time(request.startTime().toString() + "Z")
                .duration(60) // Default 60 mins
                .timezone("UTC")
                .build();

        ZoomMeetingResponse zoomResponse = zoomApiService.createMeeting(zoomRequest);

        // 2. Create Local Event
        Event event = eventMapper.toEvent(request, calendar);
        Event savedEvent = eventRepository.save(event);

        // 3. Sync with Google Calendar
        if (googleCalendarApiService != null) {
            try {
                String userId = calendar.getUserId();
                com.google.api.services.calendar.model.Event googleEvent = googleCalendarApiService.createEventWithZoom(
                        userId,
                        savedEvent.getTitle(),
                        savedEvent.getDescription(),
                        savedEvent.getStartTime(),
                        savedEvent.getEndTime(),
                        zoomResponse.getJoin_url(),
                        zoomResponse.getId(),
                        zoomResponse.getPassword());

                savedEvent.setGoogleCalendarId(googleEvent.getId());
                savedEvent.setSyncStatus(SyncStatus.SYNCED);

                Conference conference = Conference.builder()
                        .type(Conference.ConferenceType.ZOOM)
                        .conferenceLink(zoomResponse.getJoin_url())
                        .conferencePassword(zoomResponse.getPassword())
                        .platformName("Zoom")
                        .googleConferenceId(zoomResponse.getId())
                        .syncStatus(Conference.SyncStatus.SYNCED)
                        .lastSynced(LocalDateTime.now())
                        .event(savedEvent)
                        .build();

                savedEvent.setConference(conference);
                eventRepository.save(savedEvent);

                log.info("Event created with Zoom and synced to Google Calendar. Event ID: {}, Zoom ID: {}",
                        savedEvent.getId(), zoomResponse.getId());
            } catch (IOException e) {
                savedEvent.setSyncStatus(SyncStatus.PENDING);
                eventRepository.save(savedEvent);
                throw new GoogleCalendarSyncFailedException("Failed to sync Zoom event to Google Calendar" + e.getMessage());
            }
        }

        return savedEvent.getId();
    }

    @Transactional
    public void addZoomToExistingEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        // 1. Create Zoom Meeting
        ZoomMeetingRequest zoomRequest = ZoomMeetingRequest.builder()
                .topic(event.getTitle())
                .type(2)
                .start_time(event.getStartTime().toString() + "Z")
                .duration(60)
                .timezone("UTC")
                .build();

        ZoomMeetingResponse zoomResponse = zoomApiService.createMeeting(zoomRequest);

        // 2. Update Google Calendar
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                googleCalendarApiService.addZoomToExistingEvent(
                        event.getCalendar().getUserId(),
                        event.getGoogleCalendarId(),
                        zoomResponse.getJoin_url(),
                        zoomResponse.getId(),
                        zoomResponse.getPassword());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException("Failed to add Zoom to Google Calendar event" + e.getMessage());
            }
        }

        // 3. Update Local DB
        Conference conference = Conference.builder()
                .type(Conference.ConferenceType.ZOOM)
                .conferenceLink(zoomResponse.getJoin_url())
                .conferencePassword(zoomResponse.getPassword())
                .platformName("Zoom")
                .googleConferenceId(zoomResponse.getId())
                .syncStatus(Conference.SyncStatus.SYNCED)
                .lastSynced(LocalDateTime.now())
                .event(event)
                .build();

        event.setConference(conference);
        event.setSyncStatus(SyncStatus.SYNCED);
        eventRepository.save(event);
    }

    @Transactional
    public void removeZoomFromEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        if (event.getConference() == null || event.getConference().getType() != Conference.ConferenceType.ZOOM) {
            return;
        }

        Conference conference = event.getConference();

        // 1. Delete from Zoom
        if (conference.getGoogleConferenceId() != null) {
            zoomApiService.deleteMeeting(conference.getGoogleConferenceId());
        }

        // 2. Update Google Calendar
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                googleCalendarApiService.removeZoomFromEvent(
                        event.getCalendar().getUserId(),
                        event.getGoogleCalendarId());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException("Failed to remove Zoom from Google Calendar event" + e.getMessage());
            }
        }

        // 3. Update Local DB
        event.setConference(null);
        eventRepository.save(event);
        conferenceRepository.delete(conference);
    }

    private String getGoogleEventId(Event event) {
        if (event.getGoogleCalendarId() != null && !event.getGoogleCalendarId().isBlank()) {
            return event.getGoogleCalendarId();
        }
        return event.getId();
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toEventResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(String id) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }

        return eventRepository.findById(id)
                .map(eventMapper::toEventResponse)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));
    }

    public List<EventResponse> getEventsByCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new MustNotBeEmptyException("Calendar ID cannot be empty");
        }

        // Validate calendar exists
        calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        return eventRepository.findByCalendarId(calendarId)
                .stream()
                .map(eventMapper::toEventResponse)
                .collect(Collectors.toList());
    }

    public List<EventResponse> getEventsByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new MustNotBeEmptyException("User ID cannot be empty");
        }

        List<EventResponse> userEvents = eventRepository.findByCalendar_UserId(userId)
                .stream()
                .map(eventMapper::toEventResponse)
                .toList();

        if (userEvents.isEmpty()) {
            throw new NotFoundException("No events found for user: " + userId);
        }

        return userEvents;
    }

    @Transactional
    public void updateEvent(String id, EventUpdateRequest request) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        // Validate time if being updated
        LocalDateTime newStartTime = request.startTime() != null ? request.startTime() : event.getStartTime();
        LocalDateTime newEndTime = request.endTime() != null ? request.endTime() : event.getEndTime();
        boolean isAllDay = request.allDay() != null ? request.allDay() : event.isAllDay();

        if (!isAllDay && newStartTime != null && newEndTime != null) {
            if (newEndTime.isBefore(newStartTime)) {
                throw new IncorrectTimeException("End time cannot be before start time");
            }
        }

        // Update basic event fields
        eventMapper.updateEventFromRequest(event, request);

        // Handle location update
        if (request.location() != null) {
            Location location;
            if (event.getLocation() != null) {
                // Update existing location
                location = event.getLocation();
                locationMapper.updateLocationFromRequest(location, request.location());
                locationRepository.save(location);
            } else {
                // Create new location
                location = locationMapper.toLocation(request.location());
                location = locationRepository.save(location);
                event.setLocation(location);
            }
        }

        // Handle conference update
        if (request.conference() != null) {
            Conference conference;
            if (event.getConference() != null) {
                // Update existing conference
                conference = event.getConference();
                conferenceMapper.updateConferenceFromRequest(conference, request.conference());
                conferenceRepository.save(conference);
            } else {
                // Create new conference
                conference = conferenceMapper.toConference(request.conference(), event);
                conferenceRepository.save(conference);
            }
        }

        eventRepository.save(event);

        // Sync with Google Calendar if event is already synced and service is available
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String locationName = event.getLocation() != null ? event.getLocation().getPlaceName() : null;
                String userId = event.getCalendar().getUserId();

                googleCalendarApiService.updateCompleteEvent(
                        userId,
                        event.getGoogleCalendarId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartTime(),
                        event.getEndTime(),
                        locationName,
                        List.of() // No attendees support in current DTO
                );

                event.setSyncStatus(SyncStatus.SYNCED);
                eventRepository.save(event);

                log.info("Event updated and synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                event.setSyncStatus(SyncStatus.PENDING);
                eventRepository.save(event);
                throw new GoogleCalendarSyncFailedException("Failed to sync event update to Google Calendar. Event ID: {}" + event.getId() + e.getMessage());
            }
        }
    }

    @Transactional
    public void deleteEvent(String id) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        // Delete from Google Calendar if synced and service is available
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                googleCalendarApiService.deleteEvent(userId, event.getGoogleCalendarId());
                log.info("Event deleted from Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException("Failed to delete event from Google Calendar. Event ID: {}" + event.getId() + e.getMessage());
            }
        }

        eventRepository.delete(event);
    }

    @Transactional
    public String createEventWithLocation(String calendarId, EventRequest request) {
        String eventId = createEvent(calendarId, request);
        return eventId;
    }

    @Transactional
    public void attachLocation(String eventId, String locationId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + locationId));

        event.setLocation(location);
        eventRepository.save(event);
    }

    @Transactional
    public void attachConference(String eventId, String conferenceId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new NotFoundException("Conference not found with ID: " + conferenceId));

        if (conference.getEvent() != null && !conference.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Conference is already attached to another event");
        }

        conference.setEvent(event);
        conferenceRepository.save(conference);
    }
}