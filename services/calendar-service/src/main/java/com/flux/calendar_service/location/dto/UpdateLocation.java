package com.flux.calendar_service.location.dto;

public record UpdateLocation(
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
