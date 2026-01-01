package com.flux.calendar_service.event.dto;

import com.flux.calendar_service.calendar.dto.CalendarResponse;
import com.flux.calendar_service.conference.dto.ConferenceResponse;
import com.flux.calendar_service.event.EventStatus;
import com.flux.calendar_service.event.EventType;
import com.flux.calendar_service.event.SyncStatus;
import com.flux.calendar_service.location.dto.LocationResponse;

import java.time.LocalDateTime;

public record EventResponse(
                String id,
                CalendarResponse calendar,
                String title,
                String description,
                String colorHex,
                LocationResponse location,
                ConferenceResponse conference,
                EventType type,
                LocalDateTime startTime,
                LocalDateTime endTime,
                boolean allDay,
                SyncStatus syncStatus,
                EventStatus status,
                java.util.List<com.flux.calendar_service.task.dto.TaskResponse> tasks,
                java.util.List<com.flux.calendar_service.attachment.dto.AttachmentResponse> attachments,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
