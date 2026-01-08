package com.flux.calendar_service.calendar;

import com.flux.calendar_service.calendar.dto.CalendarRequest;
import com.flux.calendar_service.calendar.dto.CalendarResponse;
import com.flux.calendar_service.calendar.dto.CalendarUpdateRequest;
import com.flux.calendar_service.calendar.dto.PrimaryRequest;
import com.flux.calendar_service.exceptions.EmptyCalendarsException;
import com.flux.calendar_service.exceptions.MustBeUniqueException;
import com.flux.calendar_service.exceptions.MustNotBeEmptyException;
import com.flux.calendar_service.exceptions.SomethingWentWrongException;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final CalendarMapper calendarMapper;

    public String createCalendar(CalendarRequest request) {
        boolean isPrimary = false;
        List<Calendar> calendars = calendarRepository.findCalendarByUserIdAndIsDeletedFalse(request.userId());
        if(calendars.isEmpty()){
            isPrimary = true;
        }
        for (Calendar calendar : calendars) {
            if(calendar.getTitle().equals(request.title())){
                throw new MustBeUniqueException("The title of calendar must be unique!");
            }
        }

        String colorHex;

        if(!request.colorHex().isEmpty()){
            colorHex = request.colorHex();
        }
        else {
            colorHex = "#4285F4";
        }

        if(request.isPrimary()){
            for (Calendar calendar : calendars) {
                if(calendar.isPrimary()){
                    Calendar calendar1 = calendarRepository.findCalendarByIdAndIsDeletedFalse(calendar.getId())
                            .orElseThrow(() -> new SomethingWentWrongException("Something went wrong!"));
                    calendar1.setPrimary(false);
                    calendarRepository.save(calendar1);
                    isPrimary = true;
                    break;
                }
            }
        }

        Calendar calendar = Calendar.builder()
                .userId(request.userId())
                .title(request.title())
                .description(request.description())
                .colorHex(colorHex)
                .timezone(request.timezone())
                .isPrimary(isPrimary)
                .isDeleted(false)
                .build();

        return calendarRepository.save(calendar).getId();

    };

    public List<CalendarResponse> getAllCalendars() {
        return calendarRepository.findAllByIsDeletedFalse()
                .stream()
                .map(calendarMapper::toCalendarResponse)
                .collect(Collectors.toList());
    }

    public CalendarResponse getCalendarById(String id){
        if(id.isBlank()){
            throw new MustNotBeEmptyException("Id can not be empty");
        }


        return calendarRepository.findCalendarByIdAndIsDeletedFalse(id)
                .map(calendarMapper::toCalendarResponse)
                .orElseThrow(() -> new NotFoundException("Calendar with this id is not found!"));
    }

    public List<CalendarResponse> getCalendarsByUserId(String userId) {
        if(userId.isBlank()){
            throw new MustNotBeEmptyException("user id can not be empty!");
        }

        List<CalendarResponse> userCalendars = calendarRepository.findCalendarByUserIdAndIsDeletedFalse(userId)
                .stream()
                .map(calendarMapper::toCalendarResponse)
                .toList();

        if(userCalendars.isEmpty()){
            throw new NotFoundException("There is no any calendars by this user!");
        }

        return userCalendars;
    }

    public CalendarResponse getPrimaryCalendar(String userId) {
        List<CalendarResponse> calendars = getCalendarsByUserId(userId);

        CalendarResponse primaryCalendar = null;

        for(CalendarResponse calendar : calendars) {
            if(calendar.isPrimary()){
                primaryCalendar = calendar;
                break;
            }

            throw new NotFoundException("There is no any primary calendars in this user");
        }

        return primaryCalendar;
    }

    public CalendarResponse getByTitle(String userId, String title) {
        if(userId.isBlank() || title.isBlank()){
            throw new MustNotBeEmptyException("User's id or calendar's title can not be empty!");
        }

        List<CalendarResponse> calendars = calendarRepository.findCalendarByUserId(userId)
                .stream()
                .map(calendarMapper::toCalendarResponse)
                .toList();

        CalendarResponse output = null;

        if(calendars.isEmpty()) {
            throw new EmptyCalendarsException("Not any calendars is found!");
        }

        for(CalendarResponse calendar : calendars) {
            if(calendar.title().equals(title)){
                output = calendar;
                break;
            }
        }

        if(output == null) {
            throw new NotFoundException("Calendar with this title not found!");
        }

        return output;
    }


    public void makePrimary(String id, PrimaryRequest request) {
        List<Calendar> calendars = calendarRepository.findCalendarByUserId(request.userId());
        boolean isCalendarActive = false;
        if(calendars.isEmpty()){
            throw new NotFoundException("Calendars by this user is not found!");
        }

        for (Calendar calendar : calendars) {
            if(calendar.getId().equals(id)){
                isCalendarActive = true;
                break;
            }
        }

        if(!isCalendarActive){
            throw new NotFoundException("The calendar with this id is not found in this user!");
        }


        for (Calendar calendar : calendars) {
            if (calendar.isPrimary()){
                calendar.setPrimary(false);
                calendarRepository.save(calendar);
            }
            if(calendar.getId().equals(id)){
                calendar.setPrimary(true);
                calendarRepository.save(calendar);
                break;
            }
        }
    }

    public void updateCalendar(String id, CalendarUpdateRequest request) {
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found!"));

        mergeCalendar(calendar, request);
        calendarRepository.save(calendar);
    }

    private void mergeCalendar(Calendar calendar, CalendarUpdateRequest request) {
        if(StringUtils.isNotBlank(request.title())){
            calendar.setTitle(request.title());
        }
        if(StringUtils.isNotBlank(request.description())){
            calendar.setDescription(request.description());
        }
        if(StringUtils.isNotBlank(request.colorHex())){
            calendar.setColorHex(request.colorHex());
        }
        if(StringUtils.isNotBlank(request.timezone())){
            calendar.setTimezone(request.timezone());
        }
    }

    public void deleteCalendar(String id) {
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found!"));

        calendar.setDeleted(true);
        calendarRepository.save(calendar);
    }

    public void recoverCalendar(String id) {
        Calendar calendar = calendarRepository.findCalendarByIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found!"));

        calendar.setPrimary(false);
        calendar.setDeleted(false);
        calendarRepository.save(calendar);
    }
}