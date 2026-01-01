package com.flux.calendar_service.event.dto;

import com.flux.calendar_service.event.EventStatus;
import com.flux.calendar_service.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventRequest(
                @NotNull @NotBlank String title,
                @NotNull @NotBlank String description,
                @NotNull @NotBlank String colorHex,
                EventType type,
                LocalDate date,
                LocalDateTime startTime,
                LocalDateTime endTime,
                boolean allDay,
                EventStatus status,
                java.util.List<com.flux.calendar_service.task.dto.TaskRequest> tasks,
                java.util.List<com.flux.calendar_service.attachment.dto.AttachmentRequest> attachments) {
}
