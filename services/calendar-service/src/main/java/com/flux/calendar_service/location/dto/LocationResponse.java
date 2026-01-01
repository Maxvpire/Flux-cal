package com.flux.calendar_service.location.dto;

public record LocationResponse(
        String id,
        String placeName,
        String streetAddress,
        String city,
        String country,
        String buildingName,
        Integer floor,
        String room,
        Double latitude,
        Double longitude,
        String placeId
) {
}
