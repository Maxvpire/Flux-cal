package com.flux.calendar_service.event.dto;

import com.flux.calendar_service.conference.dto.ConferenceRequest;
import com.flux.calendar_service.event.EventStatus;
import com.flux.calendar_service.event.EventType;
import com.flux.calendar_service.location.dto.LocationRequest;

import java.time.LocalDateTime;

public record EventUpdateRequest(
                String title,
                String description,
                String colorHex,
                LocationRequest location,
                ConferenceRequest conference,
                EventType type,
                LocalDateTime startTime,
                LocalDateTime endTime,
                Boolean allDay,
                EventStatus status,
                java.util.List<com.flux.calendar_service.attachment.dto.AttachmentRequest> attachments) {
}
