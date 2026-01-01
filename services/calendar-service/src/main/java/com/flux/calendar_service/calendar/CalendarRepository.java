package com.flux.calendar_service.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, String> {
    List<Calendar> findCalendarByUserId(String userId);
    List<Calendar> findAllByIsDeletedFalse();
    Optional<Calendar> findCalendarByIdAndIsDeletedFalse(String id);
    Optional<Calendar> findCalendarByIdAndIsDeletedTrue(String id);
    List<Calendar> findCalendarByUserIdAndIsDeletedFalse(String userId);
}
