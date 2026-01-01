package com.flux.calendar_service.calendar;

import com.flux.calendar_service.calendar.dto.CalendarResponse;
import org.springframework.stereotype.Service;

@Service
public class CalendarMapper {
    public CalendarResponse toCalendarResponse(Calendar calendar) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getUserId(),
                calendar.getTitle(),
                calendar.getDescription(),
                calendar.getColorHex(),
                calendar.getTimezone(),
                calendar.isPrimary(),
                calendar.isDeleted(),
                calendar.getCreatedAt(),
                calendar.getUpdatedAt()
        );
    }
}
