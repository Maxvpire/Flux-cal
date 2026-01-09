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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private GoogleCalendarApiService googleCalendarApiService;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#result"),
        @CacheEvict(value = "calendarEvents", key = "#calendarId"),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#result != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public String createEvent(String calendarId, EventRequest request) {
        validateCalendarId(calendarId);
        
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        validateEventTime(request);

        Event event = eventMapper.toEvent(request, calendar);
        associateChildEntities(event);
        
        Event savedEvent = eventRepository.save(event);

        syncWithGoogleCalendar(savedEvent, calendar.getUserId());

        return savedEvent.getId();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#result"),
        @CacheEvict(value = "calendarEvents", key = "#calendarId"),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#result != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public String createEventWithGoogleMeet(String calendarId, EventRequest request) {
        validateCalendarId(calendarId);

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        validateEventTime(request);

        Event event = eventMapper.toEvent(request, calendar);
        associateChildEntities(event);
        
        Event savedEvent = eventRepository.save(event);

        checkGoogleCalendarEnabled();

        try {
            String userId = calendar.getUserId();
            com.google.api.services.calendar.model.Event googleEvent = createGoogleEventWithMeet(savedEvent, userId);

            Conference conference = extractAndBuildConference(googleEvent, savedEvent);

            updateEventWithGoogleData(savedEvent, googleEvent, conference);

            log.info("Event synced. Local ID: {}, Google ID: {}, Meet: {}",
                    savedEvent.getId(), googleEvent.getId(), conference.getMeetLink());

        } catch (IOException e) {
            handleGoogleSyncFailure(savedEvent, e);
        }

        return savedEvent.getId();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public void addGoogleMeetToExistingEvent(String id) {
        validateEventId(id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        checkGoogleCalendarEnabled();

        try {
            String userId = event.getCalendar().getUserId();
            com.google.api.services.calendar.model.Event googleEvent = googleCalendarApiService
                    .addGoogleMeetToExistingEvent(userId, getGoogleEventId(event));

            Conference conference = extractAndBuildConference(googleEvent, event);

            event.setConference(conference);
            event.setSyncStatus(SyncStatus.SYNCED);
            eventRepository.save(event);

            log.info("Google Meet added to event. Local ID: {}, Google ID: {}, Meet: {}",
                    event.getId(), googleEvent.getId(), conference.getMeetLink());

        } catch (IOException e) {
            log.error("Failed to add Google Meet to event. Event ID: {}", event.getId(), e);
            throw new AddGoogleMeetFailedException("Failed to add Google Meet" + e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public void removeGoogleMeetFromEvent(String id) {
        validateEventId(id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        if (event.getConference() == null) {
            return;
        }

        removeGoogleMeetFromCalendar(event);
        removeConferenceFromEvent(event);

        log.info("Google Meet removed from event. Local ID: {}", id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#result"),
        @CacheEvict(value = "calendarEvents", key = "#calendarId"),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#result != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public String createEventWithNewZoomMeeting(String calendarId, EventRequest request) {
        validateCalendarId(calendarId);

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        ZoomMeetingResponse zoomResponse = createZoomMeeting(request);

        Event event = eventMapper.toEvent(request, calendar);
        Event savedEvent = eventRepository.save(event);

        if (googleCalendarApiService != null) {
            syncZoomEventWithGoogle(savedEvent, calendar, zoomResponse);
        }

        return savedEvent.getId();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public void addZoomToExistingEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        ZoomMeetingResponse zoomResponse = createZoomMeetingForEvent(event);

        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            addZoomToGoogleCalendar(event, zoomResponse);
        }

        Conference conference = buildZoomConference(zoomResponse, event);
        event.setConference(conference);
        event.setSyncStatus(SyncStatus.SYNCED);
        eventRepository.save(event);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public void removeZoomFromEvent(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        if (event.getConference() == null || event.getConference().getType() != Conference.ConferenceType.ZOOM) {
            return;
        }

        Conference conference = event.getConference();

        deleteZoomMeeting(conference);
        removeZoomFromGoogleCalendar(event);
        removeConferenceFromEvent(event);
    }

    // Cacheable read operations

    @Cacheable(value = "allEvents", 
              key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        log.debug("Fetching all events from database, page: {}, size: {}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        Page<Event> events = eventRepository.findAll(pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Cacheable(value = "event", key = "#id", unless = "#result == null")
    public EventResponse getEventById(String id) {
        validateEventId(id);
        log.debug("Fetching event from database: {}", id);
        
        return eventRepository.findById(id)
                .map(eventMapper::toEventResponse)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));
    }

    @Cacheable(value = "calendarEvents", 
              key = "#calendarId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<EventResponse> getEventsByCalendarId(String calendarId, Pageable pageable) {
        validateCalendarId(calendarId);

        calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + calendarId));

        log.debug("Fetching events for calendar {} from database, page: {}", calendarId, pageable.getPageNumber());
        Page<Event> events = eventRepository.findByCalendarId(calendarId, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Cacheable(value = "userEvents", 
              key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<EventResponse> getEventsByUserId(String userId, Pageable pageable) {
        validateUserId(userId);

        log.debug("Fetching events for user {} from database, page: {}", userId, pageable.getPageNumber());
        Page<Event> events = eventRepository.findByCalendar_UserId(userId, pageable);
        
        if (events.isEmpty()) {
            throw new NotFoundException("No events found for user: " + userId);
        }

        return events.map(eventMapper::toEventResponse);
    }

    @Cacheable(value = "eventSearch", 
              key = "#userId + ':' + #start + ':' + #end + ':' + #keyword + ':' + #pageable")
    public Page<EventResponse> searchEvents(String userId, LocalDateTime start, 
                                           LocalDateTime end, String keyword, Pageable pageable) {
        log.debug("Searching events for user: {}, keyword: {}", userId, keyword);
        Page<Event> events = eventRepository.searchEvents(userId, start, end, keyword, pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Cacheable(value = "bulkEvents", key = "T(java.util.Arrays).toString(#ids)")
    public List<EventResponse> getEventsByIds(List<String> ids) {
        log.debug("Fetching bulk events: {}", ids);
        List<Event> events = eventRepository.findAllById(ids);
        return events.stream()
                .map(eventMapper::toEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true),
        @CacheEvict(value = "bulkEvents", allEntries = true)
    })
    public void updateEvent(String id, EventUpdateRequest request) {
        validateEventId(id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        validateUpdateTime(event, request);

        eventMapper.updateEventFromRequest(event, request);

        updateLocation(event, request);
        updateConference(event, request);

        eventRepository.save(event);

        syncEventUpdateWithGoogle(event);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#id"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true),
        @CacheEvict(value = "bulkEvents", allEntries = true)
    })
    public void deleteEvent(String id) {
        validateEventId(id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + id));

        deleteFromGoogleCalendar(event);

        eventRepository.delete(event);
        clearEventCache(id, event);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#result"),
        @CacheEvict(value = "calendarEvents", key = "#calendarId"),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#result != null"),
        @CacheEvict(value = "allEvents", allEntries = true),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public String createEventWithLocation(String calendarId, EventRequest request) {
        return createEvent(calendarId, request);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#eventId"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
    public void attachLocation(String eventId, String locationId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + locationId));

        event.setLocation(location);
        eventRepository.save(event);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "event", key = "#eventId"),
        @CacheEvict(value = "calendarEvents", 
                   key = "#event.calendar.id", 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "eventSearch", allEntries = true)
    })
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

    // Utility methods for cache management
    public void clearAllEventCache() {
        redisTemplate.delete(redisTemplate.keys("event:*"));
        redisTemplate.delete(redisTemplate.keys("events:*"));
        redisTemplate.delete(redisTemplate.keys("calendarEvents:*"));
        redisTemplate.delete(redisTemplate.keys("userEvents:*"));
        redisTemplate.delete(redisTemplate.keys("eventSearch:*"));
        redisTemplate.delete(redisTemplate.keys("allEvents:*"));
        redisTemplate.delete(redisTemplate.keys("bulkEvents:*"));
        log.info("All event cache cleared");
    }

    public void clearCacheForCalendar(String calendarId) {
        redisTemplate.delete("calendarEvents:" + calendarId + ":*");
        redisTemplate.delete("eventSearch:*" + calendarId + "*");
        log.info("Cache cleared for calendar: {}", calendarId);
    }

    public void clearCacheForUser(String userId) {
        redisTemplate.delete("userEvents:" + userId + ":*");
        redisTemplate.delete("eventSearch:" + userId + ":*");
        log.info("Cache cleared for user: {}", userId);
    }

    // Private helper methods
    private void validateCalendarId(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            throw new MustNotBeEmptyException("Calendar ID cannot be empty");
        }
    }

    private void validateEventId(String id) {
        if (id == null || id.isBlank()) {
            throw new MustNotBeEmptyException("Event ID cannot be empty");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new MustNotBeEmptyException("User ID cannot be empty");
        }
    }

    private void validateEventTime(EventRequest request) {
        if (!request.allDay() && request.startTime() != null && request.endTime() != null) {
            if (request.endTime().isBefore(request.startTime())) {
                throw new IncorrectTimeException("End time cannot be before start time");
            }
        }
    }

    private void validateUpdateTime(Event event, EventUpdateRequest request) {
        LocalDateTime newStartTime = request.startTime() != null ? request.startTime() : event.getStartTime();
        LocalDateTime newEndTime = request.endTime() != null ? request.endTime() : event.getEndTime();
        boolean isAllDay = request.allDay() != null ? request.allDay() : event.isAllDay();

        if (!isAllDay && newStartTime != null && newEndTime != null) {
            if (newEndTime.isBefore(newStartTime)) {
                throw new IncorrectTimeException("End time cannot be before start time");
            }
        }
    }

    private void associateChildEntities(Event event) {
        if (event.getTasks() != null) {
            event.getTasks().forEach(task -> task.setEvent(event));
        }
        if (event.getAttachments() != null) {
            event.getAttachments().forEach(attachment -> attachment.setEvent(event));
        }
    }

    private void checkGoogleCalendarEnabled() {
        if (googleCalendarApiService == null) {
            throw new GoogleCalendarDisabledException("Google Calendar integration is disabled");
        }
    }

    private com.google.api.services.calendar.model.Event createGoogleEventWithMeet(Event savedEvent, String userId) 
            throws IOException {
        if (savedEvent.isAllDay()) {
            return googleCalendarApiService.createAllDayEvent(
                    userId,
                    savedEvent.getTitle(),
                    savedEvent.getDescription(),
                    savedEvent.getStartTime());
        } else {
            return googleCalendarApiService.createEventWithGoogleMeet(
                    userId,
                    savedEvent.getTitle(),
                    savedEvent.getDescription(),
                    savedEvent.getStartTime(),
                    savedEvent.getEndTime(),
                    savedEvent.getType().toString());
        }
    }

    private Conference extractAndBuildConference(com.google.api.services.calendar.model.Event googleEvent, Event event) {
        if (event.isAllDay() || googleEvent.getConferenceData() == null) {
            return null;
        }

        com.google.api.services.calendar.model.ConferenceData confData = googleEvent.getConferenceData();
        String googleConferenceId = confData.getConferenceId();
        String meetLink = null;
        String meetingCode = null;
        String phoneNumber = null;
        String pin = null;

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

        return Conference.builder()
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
    }

    private void updateEventWithGoogleData(Event savedEvent, 
                                          com.google.api.services.calendar.model.Event googleEvent, 
                                          Conference conference) {
        savedEvent.setGoogleCalendarId(googleEvent.getId());
        savedEvent.setSyncStatus(SyncStatus.SYNCED);
        savedEvent.setConference(conference);
        eventRepository.save(savedEvent);
    }

    private void handleGoogleSyncFailure(Event savedEvent, IOException e) {
        savedEvent.setSyncStatus(SyncStatus.PENDING);
        eventRepository.save(savedEvent);
        throw new GoogleCalendarSyncFailedException(
                "Failed to sync event to Google Calendar. Event ID: " + savedEvent.getId() + e.getMessage());
    }

    private void removeGoogleMeetFromCalendar(Event event) {
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                googleCalendarApiService.removeConferenceFromEvent(userId, getGoogleEventId(event));
            } catch (IOException e) {
                throw new RemoveGoogleMeetFailedException(
                        "Failed to remove Google Meet from Google Calendar. Event ID: " + event.getId() + e.getMessage());
            }
        }
    }

    private void removeConferenceFromEvent(Event event) {
        Conference conference = event.getConference();
        event.setConference(null);
        eventRepository.save(event);
        conferenceRepository.delete(conference);
    }

    private ZoomMeetingResponse createZoomMeeting(EventRequest request) {
        ZoomMeetingRequest zoomRequest = ZoomMeetingRequest.builder()
                .topic(request.title())
                .type(2)
                .start_time(request.startTime().toString() + "Z")
                .duration(60)
                .timezone("UTC")
                .build();
        return zoomApiService.createMeeting(zoomRequest);
    }

    private ZoomMeetingResponse createZoomMeetingForEvent(Event event) {
        ZoomMeetingRequest zoomRequest = ZoomMeetingRequest.builder()
                .topic(event.getTitle())
                .type(2)
                .start_time(event.getStartTime().toString() + "Z")
                .duration(60)
                .timezone("UTC")
                .build();
        return zoomApiService.createMeeting(zoomRequest);
    }

    private void syncZoomEventWithGoogle(Event savedEvent, Calendar calendar, ZoomMeetingResponse zoomResponse) {
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
            throw new GoogleCalendarSyncFailedException(
                    "Failed to sync Zoom event to Google Calendar" + e.getMessage());
        }
    }

    private void addZoomToGoogleCalendar(Event event, ZoomMeetingResponse zoomResponse) {
        try {
            googleCalendarApiService.addZoomToExistingEvent(
                    event.getCalendar().getUserId(),
                    event.getGoogleCalendarId(),
                    zoomResponse.getJoin_url(),
                    zoomResponse.getId(),
                    zoomResponse.getPassword());
        } catch (IOException e) {
            throw new GoogleCalendarSyncFailedException(
                    "Failed to add Zoom to Google Calendar event" + e.getMessage());
        }
    }

    private Conference buildZoomConference(ZoomMeetingResponse zoomResponse, Event event) {
        return Conference.builder()
                .type(Conference.ConferenceType.ZOOM)
                .conferenceLink(zoomResponse.getJoin_url())
                .conferencePassword(zoomResponse.getPassword())
                .platformName("Zoom")
                .googleConferenceId(zoomResponse.getId())
                .syncStatus(Conference.SyncStatus.SYNCED)
                .lastSynced(LocalDateTime.now())
                .event(event)
                .build();
    }

    private void deleteZoomMeeting(Conference conference) {
        if (conference.getGoogleConferenceId() != null) {
            zoomApiService.deleteMeeting(conference.getGoogleConferenceId());
        }
    }

    private void removeZoomFromGoogleCalendar(Event event) {
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                googleCalendarApiService.removeZoomFromEvent(
                        event.getCalendar().getUserId(),
                        event.getGoogleCalendarId());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException(
                        "Failed to remove Zoom from Google Calendar event" + e.getMessage());
            }
        }
    }

    private String getGoogleEventId(Event event) {
        if (event.getGoogleCalendarId() != null && !event.getGoogleCalendarId().isBlank()) {
            return event.getGoogleCalendarId();
        }
        return event.getId();
    }

    private void syncWithGoogleCalendar(Event savedEvent, String userId) {
        if (googleCalendarApiService != null) {
            try {
                com.google.api.services.calendar.model.Event googleEvent;
                if (savedEvent.isAllDay()) {
                    googleEvent = googleCalendarApiService.createAllDayEvent(
                            userId,
                            savedEvent.getTitle(),
                            savedEvent.getDescription(),
                            savedEvent.getStartTime());
                } else {
                    googleEvent = googleCalendarApiService.createEvent(
                            userId,
                            savedEvent.getTitle(),
                            savedEvent.getDescription(),
                            savedEvent.getStartTime(),
                            savedEvent.getEndTime(),
                            savedEvent.getType().toString());
                }

                savedEvent.setGoogleCalendarId(googleEvent.getId());
                savedEvent.setSyncStatus(SyncStatus.SYNCED);
                eventRepository.save(savedEvent);

                log.info("Event created and synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        savedEvent.getId(), googleEvent.getId());
            } catch (IOException e) {
                savedEvent.setSyncStatus(SyncStatus.PENDING);
                eventRepository.save(savedEvent);
                throw new GoogleCalendarSyncFailedException(
                        "Failed to sync event to Google Calendar. Event ID: " + savedEvent.getId() + e.getMessage());
            }
        } else {
            throw new GoogleCalendarDisabledException("Google Calendar integration is disabled. Event created locally only.");
        }
    }

    private void updateLocation(Event event, EventUpdateRequest request) {
        if (request.location() != null) {
            Location location;
            if (event.getLocation() != null) {
                location = event.getLocation();
                locationMapper.updateLocationFromRequest(location, request.location());
                locationRepository.save(location);
            } else {
                location = locationMapper.toLocation(request.location());
                location = locationRepository.save(location);
                event.setLocation(location);
            }
        }
    }

    private void updateConference(Event event, EventUpdateRequest request) {
        if (request.conference() != null) {
            Conference conference;
            if (event.getConference() != null) {
                conference = event.getConference();
                conferenceMapper.updateConferenceFromRequest(conference, request.conference());
                conferenceRepository.save(conference);
            } else {
                conference = conferenceMapper.toConference(request.conference(), event);
                conferenceRepository.save(conference);
            }
        }
    }

    private void syncEventUpdateWithGoogle(Event event) {
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
                        List.of());

                event.setSyncStatus(SyncStatus.SYNCED);
                eventRepository.save(event);

                log.info("Event updated and synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                event.setSyncStatus(SyncStatus.PENDING);
                eventRepository.save(event);
                throw new GoogleCalendarSyncFailedException(
                        "Failed to sync event update to Google Calendar. Event ID: " + event.getId() + e.getMessage());
            }
        }
    }

    private void deleteFromGoogleCalendar(Event event) {
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                googleCalendarApiService.deleteEvent(userId, event.getGoogleCalendarId());
                log.info("Event deleted from Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException(
                        "Failed to delete event from Google Calendar. Event ID: " + event.getId() + e.getMessage());
            }
        }
    }

    private void clearEventCache(String id, Event event) {
        if (event != null && event.getCalendar() != null) {
            redisTemplate.delete("calendarEvents:" + event.getCalendar().getId() + ":*");
            redisTemplate.delete("userEvents:" + event.getCalendar().getUserId() + ":*");
        }
        redisTemplate.delete("event:" + id);
        log.debug("Cache cleared for event: {}", id);
    }
}