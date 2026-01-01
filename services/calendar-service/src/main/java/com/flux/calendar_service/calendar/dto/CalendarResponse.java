package com.flux.calendar_service.calendar.dto;

import com.flux.calendar_service.calendar.AccessLevel;

import java.time.LocalDateTime;

public record CalendarResponse(
        String id,
        String userId,
        String title,
        String description,
        String colorHex,
        String timezone,
        boolean isPrimary,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
