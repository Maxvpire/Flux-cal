package com.flux.calendar_service.conference.dto;

import com.flux.calendar_service.conference.Conference;

import java.time.LocalDateTime;

public record ConferenceResponse(
        String id,
        Conference.ConferenceType type,
        String googleConferenceId,
        String meetLink,
        String meetingCode,
        String phoneNumber,
        String pin,
        String conferenceLink,
        String conferencePassword,
        String platformName,
        Conference.SyncStatus syncStatus,
        LocalDateTime lastSynced,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
