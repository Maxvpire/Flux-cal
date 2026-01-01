package com.flux.calendar_service.location;

import com.flux.calendar_service.location.dto.LocationRequest;
import com.flux.calendar_service.location.dto.LocationResponse;
import org.springframework.stereotype.Service;

@Service
public class LocationMapper {

    public LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationResponse(
                location.getId(),
                location.getPlaceName(),
                location.getStreetAddress(),
                location.getCity(),
                location.getCountry(),
                location.getBuildingName(),
                location.getFloor(),
                location.getRoom(),
                location.getLatitude(),
                location.getLongitude(),
                location.getPlaceId());
    }

    public Location toLocation(LocationRequest request) {
        if (request == null) {
            return null;
        }

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

    public void updateLocationFromRequest(Location location, LocationRequest request) {
        if (request == null) {
            return;
        }

        if (request.placeName() != null && !request.placeName().isBlank()) {
            location.setPlaceName(request.placeName());
        }
        if (request.streetAddress() != null) {
            location.setStreetAddress(request.streetAddress());
        }
        if (request.city() != null) {
            location.setCity(request.city());
        }
        if (request.country() != null) {
            location.setCountry(request.country());
        }
        if (request.buildingName() != null) {
            location.setBuildingName(request.buildingName());
        }
        if (request.floor() != null) {
            location.setFloor(request.floor());
        }
        if (request.room() != null) {
            location.setRoom(request.room());
        }
        if (request.latitude() != null) {
            location.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            location.setLongitude(request.longitude());
        }
        if (request.placeId() != null) {
            location.setPlaceId(request.placeId());
        }
    }
}
