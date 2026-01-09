package com.flux.calendar_service.location;

import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.exceptions.GoogleCalendarSyncFailedException;
import com.flux.calendar_service.google.GoogleCalendarApiService;
import com.flux.calendar_service.location.dto.LocationRequest;
import com.flux.calendar_service.location.dto.LocationResponse;
import com.flux.calendar_service.location.dto.OpenInMapResponse;
import com.flux.calendar_service.location.dto.UpdateLocation;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final EventRepository eventRepository;
    private final GoogleCalendarApiService googleCalendarApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "location", key = "#result"),
        @CacheEvict(value = "locations", allEntries = true),
        @CacheEvict(value = "eventLocations", key = "#eventId"),
        @CacheEvict(value = "locationSearch", allEntries = true),
        @CacheEvict(value = "event", 
                   key = "#event.id", 
                   condition = "#event != null"),
        @CacheEvict(value = "calendarEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null")
    })
    public String addLocation(String eventId, LocationRequest request) {
        validateEventId(eventId);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        Location location = buildLocationFromRequest(request);
        Location savedLocation = locationRepository.save(location);

        event.setLocation(savedLocation);
        eventRepository.save(event);

        syncLocationWithGoogleCalendar(savedLocation, event);

        log.info("Location added to event. Location ID: {}, Event ID: {}", 
                savedLocation.getId(), eventId);
        
        return savedLocation.getId();
    }

    @Cacheable(value = "location", key = "#id", unless = "#result == null")
    public LocationResponse findById(String id) {
        validateId(id);
        log.debug("Fetching location from database: {}", id);
        
        return locationRepository.findById(id)
                .map(locationMapper::toLocationResponse)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + id));
    }

    @Cacheable(value = "eventLocations", key = "#eventId")
    public LocationResponse findByEventId(String eventId) {
        validateId(eventId);
        log.debug("Fetching location for event: {}", eventId);
        
        return locationRepository.findByEventId(eventId)
                .map(locationMapper::toLocationResponse)
                .orElseThrow(() -> new NotFoundException("Location not found for event ID: " + eventId));
    }

    @Cacheable(value = "locations", 
              key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<LocationResponse> findAll(Pageable pageable) {
        log.debug("Fetching all locations from database, page: {}", pageable.getPageNumber());
        return locationRepository.findAll(pageable)
                .map(locationMapper::toLocationResponse);
    }

    @Cacheable(value = "locationsByCity", key = "#city + ':' + #pageable")
    public Page<LocationResponse> findByCity(String city, Pageable pageable) {
        validateString(city, "City");
        log.debug("Fetching locations by city: {}, page: {}", city, pageable.getPageNumber());
        
        return locationRepository.findByCityContainingIgnoreCase(city, pageable)
                .map(locationMapper::toLocationResponse);
    }

    @Cacheable(value = "locationsByCountry", key = "#country + ':' + #pageable")
    public Page<LocationResponse> findByCountry(String country, Pageable pageable) {
        validateString(country, "Country");
        log.debug("Fetching locations by country: {}, page: {}", country, pageable.getPageNumber());
        
        return locationRepository.findByCountryContainingIgnoreCase(country, pageable)
                .map(locationMapper::toLocationResponse);
    }

    @Cacheable(value = "locationSearch", 
              key = "#query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<LocationResponse> searchLocations(String query, Pageable pageable) {
        validateString(query, "Search query");
        log.debug("Searching locations with query: {}, page: {}", query, pageable.getPageNumber());
        
        return locationRepository.searchLocations(query, pageable)
                .map(locationMapper::toLocationResponse);
    }

    @Cacheable(value = "nearbyLocations", 
              key = "#latitude + ':' + #longitude + ':' + #radius + ':' + #pageable")
    public Page<LocationResponse> findNearbyLocations(Double latitude, Double longitude, 
                                                     Double radius, Pageable pageable) {
        validateCoordinates(latitude, longitude);
        
        log.debug("Finding nearby locations for coordinates: ({}, {}), radius: {} km", 
                 latitude, longitude, radius);
        
        return locationRepository.findNearbyLocations(latitude, longitude, radius, pageable)
                .map(locationMapper::toLocationResponse);
    }

    public OpenInMapResponse openInMaps(String id) {
        validateId(id);
        
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + id));

        String address = buildAddressString(location);
        String encodedAddress = address.replace(" ", "+");
        
        return OpenInMapResponse.builder()
                .googleMapsUrl("https://www.google.com/maps/search/" + encodedAddress)
                .yandexMapsWebUrl("https://maps.yandex.ru/?text=" + encodedAddress)
                .yandexMapsAppUrl("yandexmaps://maps.yandex.ru/?text=" + encodedAddress)
                .build();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "location", key = "#id"),
        @CacheEvict(value = "locations", allEntries = true),
        @CacheEvict(value = "locationSearch", allEntries = true),
        @CacheEvict(value = "locationsByCity", allEntries = true),
        @CacheEvict(value = "locationsByCountry", allEntries = true),
        @CacheEvict(value = "nearbyLocations", allEntries = true),
        @CacheEvict(value = "eventLocations", allEntries = true),
        @CacheEvict(value = "event", 
                   key = "#event.id", 
                   condition = "#event != null"),
        @CacheEvict(value = "calendarEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null")
    })
    public void updateLocation(String id, UpdateLocation request) {
        validateId(id);
        
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + id));

        mergeLocationWithRequest(location, request);
        Location updatedLocation = locationRepository.save(location);

        // Find and sync with associated event
        eventRepository.findEventByLocationId(id).ifPresent(event -> {
            syncLocationWithGoogleCalendar(updatedLocation, event);
            log.info("Location updated and synced with event. Location ID: {}, Event ID: {}", 
                    id, event.getId());
        });
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "location", key = "#id"),
        @CacheEvict(value = "locations", allEntries = true),
        @CacheEvict(value = "locationSearch", allEntries = true),
        @CacheEvict(value = "locationsByCity", allEntries = true),
        @CacheEvict(value = "locationsByCountry", allEntries = true),
        @CacheEvict(value = "nearbyLocations", allEntries = true),
        @CacheEvict(value = "eventLocations", allEntries = true),
        @CacheEvict(value = "event", 
                   key = "#event.id", 
                   condition = "#event != null"),
        @CacheEvict(value = "calendarEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#event != null and #event.calendar != null")
    })
    public void deleteLocation(String id) {
        validateId(id);
        
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found with ID: " + id));

        // Find associated event before deletion
        Event event = eventRepository.findEventByLocationId(id)
                .orElseThrow(() -> new NotFoundException("No event associated with location ID: " + id));

        // Remove location from event
        event.setLocation(null);
        eventRepository.save(event);

        // Delete location
        locationRepository.delete(location);

        // Sync removal with Google Calendar
        syncLocationWithGoogleCalendar(null, event);

        log.info("Location deleted and removed from event. Location ID: {}, Event ID: {}", 
                id, event.getId());
    }

    // Cache management methods
    public void clearLocationCache(String locationId) {
        redisTemplate.delete("location:" + locationId);
        redisTemplate.delete(redisTemplate.keys("eventLocations:*"));
        log.debug("Cache cleared for location: {}", locationId);
    }

    public void clearAllLocationCache() {
        redisTemplate.delete(redisTemplate.keys("location:*"));
        redisTemplate.delete(redisTemplate.keys("locations:*"));
        redisTemplate.delete(redisTemplate.keys("locationSearch:*"));
        redisTemplate.delete(redisTemplate.keys("locationsByCity:*"));
        redisTemplate.delete(redisTemplate.keys("locationsByCountry:*"));
        redisTemplate.delete(redisTemplate.keys("nearbyLocations:*"));
        redisTemplate.delete(redisTemplate.keys("eventLocations:*"));
        log.info("All location cache cleared");
    }

    // Private helper methods
    private void validateId(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
    }

    private void validateEventId(String eventId) {
        if (StringUtils.isBlank(eventId)) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
    }

    private void validateString(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude cannot be null");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private Location buildLocationFromRequest(LocationRequest request) {
        return Location.builder()
                .placeName(request.placeName())
                .streetAddress(request.streetAddress())
                .city(request.city())
                .country(request.country())
                .buildingName(request.buildingName())
                .floor(request.floor())
                .room(request.room())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .placeId(request.placeId())
                .build();
    }

    private String buildAddressString(Location location) {
        StringBuilder sb = new StringBuilder();
        
        if (StringUtils.isNotBlank(location.getPlaceName())) {
            sb.append(location.getPlaceName());
        }
        
        if (StringUtils.isNotBlank(location.getStreetAddress())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(location.getStreetAddress());
        }
        
        if (StringUtils.isNotBlank(location.getCity())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(location.getCity());
        }
        
        if (StringUtils.isNotBlank(location.getCountry())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(location.getCountry());
        }
        
        return sb.toString();
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return null;
        }
        return buildAddressString(location);
    }

    private void mergeLocationWithRequest(Location location, UpdateLocation request) {
        if (StringUtils.isNotBlank(request.placeName())) {
            location.setPlaceName(request.placeName());
        }
        if (StringUtils.isNotBlank(request.streetAddress())) {
            location.setStreetAddress(request.streetAddress());
        }
        if (StringUtils.isNotBlank(request.city())) {
            location.setCity(request.city());
        }
        if (StringUtils.isNotBlank(request.country())) {
            location.setCountry(request.country());
        }
        if (StringUtils.isNotBlank(request.buildingName())) {
            location.setBuildingName(request.buildingName());
        }
        if (request.floor() != null) {
            location.setFloor(request.floor());
        }
        if (StringUtils.isNotBlank(request.room())) {
            location.setRoom(request.room());
        }
        if (request.latitude() != null) {
            location.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            location.setLongitude(request.longitude());
        }
        if (StringUtils.isNotBlank(request.placeId())) {
            location.setPlaceId(request.placeId());
        }
    }

    private void syncLocationWithGoogleCalendar(Location location, Event event) {
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                String locationString = formatLocation(location);

                googleCalendarApiService.updateEventLocation(
                        userId,
                        event.getGoogleCalendarId(),
                        locationString
                );

                log.info("Event location synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                log.error("Failed to sync event location to Google Calendar. Event ID: {}", 
                        event.getId(), e);
                throw new GoogleCalendarSyncFailedException(
                        "Failed to sync event location update to Google Calendar. Event ID: " + 
                        event.getId() + ". Error: " + e.getMessage());
            }
        }
    }
}