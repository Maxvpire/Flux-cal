package com.flux.calendar_service.calendar.dto;

public record CalendarUpdateRequest(
    String title,
    String description,
    String colorHex,
    String timezone
) {
}
