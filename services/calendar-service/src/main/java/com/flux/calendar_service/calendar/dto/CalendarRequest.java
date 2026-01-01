package com.flux.calendar_service.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CalendarRequest(
        @NotEmpty
        @NotBlank
        String userId,
        @NotEmpty
        @NotBlank
        String title,
        String description,
        String colorHex,
        @NotEmpty
        @NotBlank
        String timezone,
        boolean isPrimary

) {
}
