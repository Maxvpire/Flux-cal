package com.flux.calendar_service.location.dto;

public record OpenInMapResponse(
        String googleMaps,
        String yandexMaps,
        String app
) {
}
