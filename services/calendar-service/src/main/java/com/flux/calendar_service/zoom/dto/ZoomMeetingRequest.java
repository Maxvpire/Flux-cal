package com.flux.calendar_service.zoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoomMeetingRequest {
    private String topic;
    private int type; // 2 for scheduled meeting
    private String start_time;
    private int duration;
    private String timezone;
    private String password;
    private ZoomMeetingSettings settings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoomMeetingSettings {
        private boolean host_video;
        private boolean participant_video;
        private boolean join_before_host;
        private boolean mute_upon_entry;
        private String auto_recording;
    }
}
