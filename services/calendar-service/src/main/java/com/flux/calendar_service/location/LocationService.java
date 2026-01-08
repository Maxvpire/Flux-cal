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

    @Transactional
    public String addLocation(String eventId, LocationRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found!"));

        Location location = Location.builder()
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

        Location newLocation = locationRepository.save(location);

        event.setLocation(newLocation);
        eventRepository.save(event);

        // Sync with Google Calendar if event is already synced and service is available
        googleCalendarSync(newLocation, event);

        return newLocation.getId();
    }

    private String formatLocation(Location location) {
        if(location != null){
            StringBuilder sb = new StringBuilder();
            sb.append(location.getPlaceName());

            if (location.getStreetAddress() != null && !location.getStreetAddress().isBlank()) {
                sb.append(", ").append(location.getStreetAddress());
            }
            if (location.getCity() != null && !location.getCity().isBlank()) {
                sb.append(", ").append(location.getCity());
            }
            if (location.getCountry() != null && !location.getCountry().isBlank()) {
                sb.append(", ").append(location.getCountry());
            }

            return sb.toString();
        }
        return null;
    }


    public OpenInMapResponse openInMaps(String id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found!"));

        return new OpenInMapResponse(
                "https://www.google.com/maps/search/" + (location.getPlaceName() + "+" + location.getStreetAddress() + "+" + location.getCity() + "+" + location.getCountry()).replace(" ", "+"),
                "https://maps.yandex.ru/?text=" + (location.getPlaceName() + "+" + location.getStreetAddress() + "+" + location.getCity() + "+" + location.getCountry()).replace(" ", "+"),
                "yandexmaps://maps.yandex.ru/?text=" + (location.getPlaceName() + "+" + location.getStreetAddress() + "+" + location.getCity() + "+" + location.getCountry()).replace(" ", "+")
        );
    }

    public LocationResponse findById(String id) {
        if(id.isBlank()){
            throw new RuntimeException("Id van not be null!");
        }

        return locationRepository.findById(id)
                .map(locationMapper::toLocationResponse)
                .orElseThrow(() -> new NotFoundException("Location not found!"));
    }

    public List<LocationResponse> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(locationMapper::toLocationResponse)
                .collect(Collectors.toList());
    }

    public void updateLocation(String id, UpdateLocation location) {
        Location location1 = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found!"));

        mergeLocation(location1, location);
        Location newLocation = locationRepository.save(location1);

        Event event = eventRepository.findEventByLocationId(location1.getId())
                .orElseThrow(() -> new NotFoundException("Event not found!"));
        googleCalendarSync(newLocation, event);

    }

    private void googleCalendarSync(Location newLocation, Event event) {
        if (googleCalendarApiService != null && event.getGoogleCalendarId() != null) {
            try {
                String userId = event.getCalendar().getUserId();
                String locationString = formatLocation(newLocation);

                googleCalendarApiService.updateEventLocation(
                        userId,
                        event.getGoogleCalendarId(),
                        locationString
                );

                log.info("Event location updated and synced to Google Calendar. Event ID: {}, Google Calendar ID: {}",
                        event.getId(), event.getGoogleCalendarId());
            } catch (IOException e) {
                throw new GoogleCalendarSyncFailedException("Failed to sync event location update to Google Calendar. Event ID: {}" + event.getId() + e.getMessage());
            }
        }
    }

    private void mergeLocation(Location location, UpdateLocation request) {
        if(StringUtils.isNotBlank(request.placeName())){
            location.setPlaceName(request.placeName());
        }
        if(StringUtils.isNotBlank(request.streetAddress())){
            location.setStreetAddress(request.streetAddress());
        }
        if(StringUtils.isNotBlank(request.city())){
            location.setCity(request.city());
        }
        if(StringUtils.isNotBlank(request.country())){
            location.setCountry(request.country());
        }
        if(StringUtils.isNotBlank(request.buildingName())){
            location.setBuildingName(request.buildingName());
        }
        if(StringUtils.isNotBlank(String.valueOf(request.floor()))){
            location.setFloor(request.floor());
        }
        if(StringUtils.isNotBlank(request.room())){
            location.setRoom(request.room());
        }
        if(StringUtils.isNotBlank(String.valueOf(request.latitude()))){
            location.setLatitude(request.latitude());
        }
        if(StringUtils.isNotBlank(String.valueOf(request.longitude()))){
            location.setLongitude(request.longitude());
        }
        if(StringUtils.isNotBlank(String.valueOf(request.placeId()))){
            location.setPlaceId(request.placeId());
        }
    }


    public void deleteLocation(String id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found!"));

        Event event = eventRepository.findEventByLocationId(location.getId())
                .orElseThrow(() -> new NotFoundException("Event not found!"));

        event.setLocation(null);
        eventRepository.save(event);
        locationRepository.delete(location);
        googleCalendarSync(null, event);
    }
    
}
