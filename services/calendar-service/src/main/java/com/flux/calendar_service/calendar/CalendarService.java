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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {
    private final CalendarRepository calendarRepository;
    private final CalendarMapper calendarMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "calendar", key = "#result"),
        @CacheEvict(value = "userCalendars", key = "#request.userId()"),
        @CacheEvict(value = "userPrimaryCalendar", key = "#request.userId()"),
        @CacheEvict(value = "allCalendars", allEntries = true),
        @CacheEvict(value = "calendarByTitle", allEntries = true),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#request.userId() != null"),
        @CacheEvict(value = "calendarEvents", allEntries = true, condition = "#result != null")
    })
    public String createCalendar(CalendarRequest request) {
        validateCalendarRequest(request);
        
        String userId = request.userId();
        List<Calendar> existingCalendars = calendarRepository.findCalendarByUserIdAndIsDeletedFalse(userId);
        
        validateUniqueTitle(existingCalendars, request.title());
        
        boolean isPrimary = determineIfPrimary(existingCalendars, request);
        String colorHex = determineColorHex(request);
        
        handlePrimaryCalendarUpdate(existingCalendars, request, isPrimary);
        
        Calendar calendar = buildCalendar(request, colorHex, isPrimary);
        Calendar savedCalendar = calendarRepository.save(calendar);
        
        log.info("Calendar created successfully. ID: {}, User: {}, Title: {}", 
                savedCalendar.getId(), userId, savedCalendar.getTitle());
        
        return savedCalendar.getId();
    }

    @Cacheable(value = "allCalendars", 
              key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CalendarResponse> getAllCalendars(Pageable pageable) {
        log.debug("Fetching all calendars from database, page: {}", pageable.getPageNumber());
        Page<Calendar> calendars = calendarRepository.findAllByIsDeletedFalse(pageable);
        return calendars.map(calendarMapper::toCalendarResponse);
    }

    @Cacheable(value = "calendar", key = "#id", unless = "#result == null")
    public CalendarResponse getCalendarById(String id) {
        validateId(id, "Calendar ID");
        log.debug("Fetching calendar from database: {}", id);
        
        return calendarRepository.findCalendarByIdAndIsDeletedFalse(id)
                .map(calendarMapper::toCalendarResponse)
                .orElseThrow(() -> new NotFoundException("Calendar with ID " + id + " not found"));
    }

    @Cacheable(value = "userCalendars", 
              key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<CalendarResponse> getCalendarsByUserId(String userId, Pageable pageable) {
        validateId(userId, "User ID");
        log.debug("Fetching calendars for user: {}, page: {}", userId, pageable.getPageNumber());
        
        Page<Calendar> calendars = calendarRepository.findCalendarByUserIdAndIsDeletedFalse(userId, pageable);
        
        if (calendars.isEmpty()) {
            throw new NotFoundException("No calendars found for user: " + userId);
        }
        
        return calendars.map(calendarMapper::toCalendarResponse);
    }

    @Cacheable(value = "userPrimaryCalendar", key = "#userId", unless = "#result == null")
    public CalendarResponse getPrimaryCalendar(String userId) {
        validateId(userId, "User ID");
        log.debug("Fetching primary calendar for user: {}", userId);
        
        return calendarRepository.findPrimaryCalendarByUserId(userId)
                .map(calendarMapper::toCalendarResponse)
                .orElseThrow(() -> new NotFoundException("No primary calendar found for user: " + userId));
    }

    @Cacheable(value = "calendarByTitle", 
              key = "#userId + ':' + #title", unless = "#result == null")
    public CalendarResponse getByTitle(String userId, String title) {
        validateId(userId, "User ID");
        validateString(title, "Calendar title");
        log.debug("Fetching calendar by title for user: {}, title: {}", userId, title);
        
        return calendarRepository.findCalendarByUserIdAndTitleAndIsDeletedFalse(userId, title)
                .map(calendarMapper::toCalendarResponse)
                .orElseThrow(() -> new NotFoundException(
                        "Calendar with title '" + title + "' not found for user: " + userId));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "calendar", key = "#id"),
        @CacheEvict(value = "userCalendars", key = "#request.userId()"),
        @CacheEvict(value = "userPrimaryCalendar", key = "#request.userId()"),
        @CacheEvict(value = "allCalendars", allEntries = true),
        @CacheEvict(value = "calendarByTitle", allEntries = true),
        @CacheEvict(value = "userEvents", allEntries = true, condition = "#request.userId() != null"),
        @CacheEvict(value = "calendarEvents", key = "#id")
    })
    public void makePrimary(String id, PrimaryRequest request) {
        validateId(id, "Calendar ID");
        validateId(request.userId(), "User ID");
        
        List<Calendar> userCalendars = calendarRepository.findCalendarByUserIdAndIsDeletedFalse(request.userId());
        
        if (userCalendars.isEmpty()) {
            throw new NotFoundException("No calendars found for user: " + request.userId());
        }
        
        Calendar targetCalendar = findCalendarById(userCalendars, id);
        
        if (targetCalendar == null) {
            throw new NotFoundException("Calendar with ID " + id + " not found in user's calendars");
        }
        
        updatePrimaryCalendars(userCalendars, targetCalendar);
        
        log.info("Calendar {} set as primary for user: {}", id, request.userId());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "calendar", key = "#id"),
        @CacheEvict(value = "userCalendars", 
                   key = "#calendar.userId", 
                   condition = "#calendar != null"),
        @CacheEvict(value = "userPrimaryCalendar", 
                   key = "#calendar.userId", 
                   condition = "#calendar != null and #calendar.primary"),
        @CacheEvict(value = "allCalendars", allEntries = true),
        @CacheEvict(value = "calendarByTitle", allEntries = true),
        @CacheEvict(value = "calendarEvents", key = "#id"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#calendar != null")
    })
    public void updateCalendar(String id, CalendarUpdateRequest request) {
        validateId(id, "Calendar ID");
        
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + id));
        
        validateTitleUniquenessOnUpdate(calendar, request);
        
        mergeCalendar(calendar, request);
        Calendar updatedCalendar = calendarRepository.save(calendar);
        
        log.info("Calendar updated successfully. ID: {}, User: {}", 
                updatedCalendar.getId(), updatedCalendar.getUserId());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "calendar", key = "#id"),
        @CacheEvict(value = "userCalendars", 
                   key = "#calendar.userId", 
                   condition = "#calendar != null"),
        @CacheEvict(value = "userPrimaryCalendar", 
                   key = "#calendar.userId", 
                   condition = "#calendar != null and #calendar.primary"),
        @CacheEvict(value = "allCalendars", allEntries = true),
        @CacheEvict(value = "calendarByTitle", allEntries = true),
        @CacheEvict(value = "calendarEvents", key = "#id"),
        @CacheEvict(value = "userEvents", 
                   allEntries = true, 
                   condition = "#calendar != null"),
        @CacheEvict(value = "event", allEntries = true, condition = "#calendar != null")
    })
    public void deleteCalendar(String id) {
        validateId(id, "Calendar ID");
        
        Calendar calendar = calendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found with ID: " + id));
        
        calendar.setDeleted(true);
        calendarRepository.save(calendar);
        
        log.info("Calendar marked as deleted. ID: {}, User: {}", 
                calendar.getId(), calendar.getUserId());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "calendar", key = "#id"),
        @CacheEvict(value = "userCalendars", key = "#calendar.userId"),
        @CacheEvict(value = "allCalendars", allEntries = true),
        @CacheEvict(value = "calendarByTitle", allEntries = true)
    })
    public void recoverCalendar(String id) {
        validateId(id, "Calendar ID");
        
        Calendar calendar = calendarRepository.findCalendarByIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new NotFoundException("Deleted calendar not found with ID: " + id));
        
        calendar.setPrimary(false);
        calendar.setDeleted(false);
        calendarRepository.save(calendar);
        
        log.info("Calendar recovered. ID: {}, User: {}", 
                calendar.getId(), calendar.getUserId());
    }

    @Cacheable(value = "bulkCalendars", key = "T(java.util.Arrays).toString(#ids)")
    public List<CalendarResponse> getCalendarsByIds(List<String> ids) {
        log.debug("Fetching bulk calendars: {}", ids);
        List<Calendar> calendars = calendarRepository.findAllByIdInAndIsDeletedFalse(ids);
        return calendars.stream()
                .map(calendarMapper::toCalendarResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "calendarSearch", 
              key = "#userId + ':' + #query + ':' + #pageable")
    public Page<CalendarResponse> searchCalendars(String userId, String query, Pageable pageable) {
        validateId(userId, "User ID");
        log.debug("Searching calendars for user: {}, query: {}", userId, query);
        
        Page<Calendar> calendars = calendarRepository.searchCalendars(userId, query, pageable);
        return calendars.map(calendarMapper::toCalendarResponse);
    }

    // Cache management methods
    public void clearCalendarCache(String calendarId) {
        redisTemplate.delete("calendar:" + calendarId);
        redisTemplate.delete("calendarEvents:" + calendarId + ":*");
        log.debug("Cache cleared for calendar: {}", calendarId);
    }

    public void clearUserCalendarCache(String userId) {
        redisTemplate.delete("userCalendars:" + userId + ":*");
        redisTemplate.delete("userPrimaryCalendar:" + userId);
        redisTemplate.delete("calendarByTitle:" + userId + ":*");
        redisTemplate.delete("calendarSearch:" + userId + ":*");
        log.debug("Cache cleared for user's calendars: {}", userId);
    }

    public void clearAllCalendarCache() {
        redisTemplate.delete(redisTemplate.keys("calendar:*"));
        redisTemplate.delete(redisTemplate.keys("allCalendars:*"));
        redisTemplate.delete(redisTemplate.keys("userCalendars:*"));
        redisTemplate.delete(redisTemplate.keys("userPrimaryCalendar:*"));
        redisTemplate.delete(redisTemplate.keys("calendarByTitle:*"));
        redisTemplate.delete(redisTemplate.keys("bulkCalendars:*"));
        redisTemplate.delete(redisTemplate.keys("calendarSearch:*"));
        log.info("All calendar cache cleared");
    }

    // Private helper methods
    private void validateCalendarRequest(CalendarRequest request) {
        validateId(request.userId(), "User ID");
        validateString(request.title(), "Calendar title");
        validateString(request.timezone(), "Timezone");
    }

    private void validateId(String id, String fieldName) {
        if (StringUtils.isBlank(id)) {
            throw new MustNotBeEmptyException(fieldName + " cannot be empty");
        }
    }

    private void validateString(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new MustNotBeEmptyException(fieldName + " cannot be empty");
        }
    }

    private void validateUniqueTitle(List<Calendar> existingCalendars, String newTitle) {
        for (Calendar calendar : existingCalendars) {
            if (calendar.getTitle().equals(newTitle)) {
                throw new MustBeUniqueException("The title of calendar must be unique!");
            }
        }
    }

    private boolean determineIfPrimary(List<Calendar> existingCalendars, CalendarRequest request) {
        if (existingCalendars.isEmpty()) {
            return true; // First calendar is automatically primary
        }
        return request.isPrimary();
    }

    private String determineColorHex(CalendarRequest request) {
        if (StringUtils.isNotBlank(request.colorHex())) {
            return request.colorHex();
        }
        return "#4285F4"; // Default Google blue
    }

    private void handlePrimaryCalendarUpdate(List<Calendar> existingCalendars, CalendarRequest request, boolean isPrimary) {
        if (request.isPrimary() && isPrimary) {
            for (Calendar calendar : existingCalendars) {
                if (calendar.isPrimary()) {
                    calendar.setPrimary(false);
                    calendarRepository.save(calendar);
                    log.debug("Previous primary calendar updated. ID: {}", calendar.getId());
                    break;
                }
            }
        }
    }

    private Calendar buildCalendar(CalendarRequest request, String colorHex, boolean isPrimary) {
        return Calendar.builder()
                .userId(request.userId())
                .title(request.title())
                .description(request.description())
                .colorHex(colorHex)
                .timezone(request.timezone())
                .isPrimary(isPrimary)
                .isDeleted(false)
                .build();
    }

    private Calendar findCalendarById(List<Calendar> calendars, String id) {
        return calendars.stream()
                .filter(calendar -> calendar.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void updatePrimaryCalendars(List<Calendar> calendars, Calendar targetCalendar) {
        for (Calendar calendar : calendars) {
            if (calendar.isPrimary() && !calendar.getId().equals(targetCalendar.getId())) {
                calendar.setPrimary(false);
                calendarRepository.save(calendar);
                log.debug("Previous primary calendar demoted. ID: {}", calendar.getId());
            }
        }
        
        targetCalendar.setPrimary(true);
        calendarRepository.save(targetCalendar);
    }

    private void validateTitleUniquenessOnUpdate(Calendar calendar, CalendarUpdateRequest request) {
        if (StringUtils.isNotBlank(request.title()) && !request.title().equals(calendar.getTitle())) {
            List<Calendar> userCalendars = calendarRepository
                    .findCalendarByUserIdAndIsDeletedFalse(calendar.getUserId());
            
            for (Calendar userCalendar : userCalendars) {
                if (userCalendar.getId().equals(calendar.getId())) {
                    continue; // Skip the calendar being updated
                }
                if (userCalendar.getTitle().equals(request.title())) {
                    throw new MustBeUniqueException("Calendar title must be unique for this user");
                }
            }
        }
    }

    private void mergeCalendar(Calendar calendar, CalendarUpdateRequest request) {
        if (StringUtils.isNotBlank(request.title())) {
            calendar.setTitle(request.title());
        }
        if (request.description() != null) {
            calendar.setDescription(request.description());
        }
        if (StringUtils.isNotBlank(request.colorHex())) {
            calendar.setColorHex(request.colorHex());
        }
        if (StringUtils.isNotBlank(request.timezone())) {
            calendar.setTimezone(request.timezone());
        }
    }
}