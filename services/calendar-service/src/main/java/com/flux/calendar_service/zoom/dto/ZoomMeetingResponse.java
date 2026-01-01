package com.flux.calendar_service.zoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoomMeetingResponse {
    private String id;
    private String join_url;
    private String password;
    private String topic;
    private String start_time;
    private int duration;
}
