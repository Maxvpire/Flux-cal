package com.flux.calendar_service.conference;

import com.flux.calendar_service.conference.dto.ConferenceRequest;
import com.flux.calendar_service.conference.dto.ConferenceResponse;
import com.flux.calendar_service.event.Event;
import org.springframework.stereotype.Service;

@Service
public class ConferenceMapper {

    public ConferenceResponse toConferenceResponse(Conference conference) {
        if (conference == null) {
            return null;
        }

        return new ConferenceResponse(
                conference.getId(),
                conference.getType(),
                conference.getGoogleConferenceId(),
                conference.getMeetLink(),
                conference.getMeetingCode(),
                conference.getPhoneNumber(),
                conference.getPin(),
                conference.getConferenceLink(),
                conference.getConferencePassword(),
                conference.getPlatformName(),
                conference.getSyncStatus(),
                conference.getLastSynced(),
                conference.getCreatedAt(),
                conference.getUpdatedAt());
    }

    public Conference toConference(ConferenceRequest request, Event event) {
        if (request == null) {
            return null;
        }

        return Conference.builder()
                .event(event)
                .type(request.type())
                .googleConferenceId(request.googleConferenceId())
                .meetLink(request.meetLink())
                .meetingCode(request.meetingCode())
                .phoneNumber(request.phoneNumber())
                .pin(request.pin())
                .conferenceLink(request.conferenceLink())
                .conferencePassword(request.conferencePassword())
                .platformName(request.platformName())
                .syncStatus(request.syncStatus())
                .build();
    }

    public void updateConferenceFromRequest(Conference conference, ConferenceRequest request) {
        if (request == null) {
            return;
        }

        if (request.type() != null) {
            conference.setType(request.type());
        }
        if (request.googleConferenceId() != null) {
            conference.setGoogleConferenceId(request.googleConferenceId());
        }
        if (request.meetLink() != null) {
            conference.setMeetLink(request.meetLink());
        }
        if (request.meetingCode() != null) {
            conference.setMeetingCode(request.meetingCode());
        }
        if (request.phoneNumber() != null) {
            conference.setPhoneNumber(request.phoneNumber());
        }
        if (request.pin() != null) {
            conference.setPin(request.pin());
        }
        if (request.conferenceLink() != null) {
            conference.setConferenceLink(request.conferenceLink());
        }
        if (request.conferencePassword() != null) {
            conference.setConferencePassword(request.conferencePassword());
        }
        if (request.platformName() != null) {
            conference.setPlatformName(request.platformName());
        }
        if (request.syncStatus() != null) {
            conference.setSyncStatus(request.syncStatus());
        }
    }
}
