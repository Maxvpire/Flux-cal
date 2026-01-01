package com.flux.calendar_service.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotNull @NotBlank String placeName,
        String streetAddress,
        String city,
        String country,
        String buildingName,
        Integer floor,
        String room,
        Double latitude,
        Double longitude,
        String placeId) {
}
