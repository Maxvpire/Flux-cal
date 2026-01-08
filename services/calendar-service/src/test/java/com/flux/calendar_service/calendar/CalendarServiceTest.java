package com.flux.calendar_service.calendar;

import com.flux.calendar_service.calendar.dto.CalendarRequest;
import com.flux.calendar_service.calendar.dto.CalendarResponse;
import com.flux.calendar_service.calendar.dto.CalendarUpdateRequest;
import com.flux.calendar_service.exceptions.MustBeUniqueException;
import com.flux.calendar_service.exceptions.MustNotBeEmptyException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private CalendarMapper calendarMapper;

    @InjectMocks
    private CalendarService calendarService;

    private Calendar calendar;
    private CalendarRequest calendarRequest;

    @BeforeEach
    void setUp() {
        calendar = Calendar.builder()
                .id("cal-1")
                .userId("user-1")
                .title("My Calendar")
                .isPrimary(true)
                .isDeleted(false)
                .build();

        calendarRequest = new CalendarRequest(
                "user-1",
                "My Calendar",
                "Description",
                "#ffffff",
                "UTC",
                true
        );
    }

    @Test
    void createCalendar_Success_FirstCalendarBecomesPrimary() {
        // Arrange
        when(calendarRepository.findCalendarByUserIdAndIsDeletedFalse("user-1")).thenReturn(Collections.emptyList());
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> {
            Calendar c = invocation.getArgument(0);
            c.setId("cal-1");
            return c;
        });

        // Act
        String id = calendarService.createCalendar(calendarRequest);

        // Assert
        assertEquals("cal-1", id);
        verify(calendarRepository).save(any(Calendar.class));
    }

    @Test
    void createCalendar_DuplicateTitle_ThrowsException() {
        // Arrange
        when(calendarRepository.findCalendarByUserIdAndIsDeletedFalse("user-1")).thenReturn(List.of(calendar));

        // Act & Assert
        assertThrows(MustBeUniqueException.class, () ->
                calendarService.createCalendar(calendarRequest)
        );
    }

    @Test
    void getCalendarById_Success() {
        // Arrange
        when(calendarRepository.findCalendarByIdAndIsDeletedFalse("cal-1")).thenReturn(Optional.of(calendar));
        CalendarResponse response = new CalendarResponse("cal-1", "user-1", "My Calendar", "Desc", "#fff", "UTC", true, false, null, null);
        when(calendarMapper.toCalendarResponse(calendar)).thenReturn(response);

        // Act
        CalendarResponse result = calendarService.getCalendarById("cal-1");

        // Assert
        assertEquals("cal-1", result.id());
    }

    @Test
    void deleteCalendar_Success() {
        // Arrange
        when(calendarRepository.findById("cal-1")).thenReturn(Optional.of(calendar));

        // Act
        calendarService.deleteCalendar("cal-1");

        // Assert
        assertTrue(calendar.isDeleted());
        verify(calendarRepository).save(calendar);
    }

    @Test
    void recoverCalendar_Success() {
        // Arrange
        calendar.setDeleted(true);
        when(calendarRepository.findCalendarByIdAndIsDeletedTrue("cal-1")).thenReturn(Optional.of(calendar));

        // Act
        calendarService.recoverCalendar("cal-1");

        // Assert
        assertFalse(calendar.isDeleted());
        verify(calendarRepository).save(calendar);
    }

    @Test
    void updateCalendar_Success() {
        // Arrange
        CalendarUpdateRequest updateRequest = new CalendarUpdateRequest("New Title", "New Desc", "#000", "UTC");
        when(calendarRepository.findById("cal-1")).thenReturn(Optional.of(calendar));

        // Act
        calendarService.updateCalendar("cal-1", updateRequest);

        // Assert
        assertEquals("New Title", calendar.getTitle());
        verify(calendarRepository).save(calendar);
    }
}
