package com.flux.calendar_service.conference.dto;

import com.flux.calendar_service.conference.Conference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConferenceRequest(
        @NotNull Conference.ConferenceType type,
        String googleConferenceId,
        String meetLink,
        String meetingCode,
        String phoneNumber,
        String pin,
        String conferenceLink,
        String conferencePassword,
        String platformName,
        Conference.SyncStatus syncStatus) {
}
