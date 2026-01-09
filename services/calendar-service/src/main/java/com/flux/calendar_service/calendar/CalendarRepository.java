package com.flux.calendar_service.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, String> {
    List<Calendar> findCalendarByUserIdAndIsDeletedFalse(String userId);
    
    Page<Calendar> findCalendarByUserIdAndIsDeletedFalse(String userId, Pageable pageable);
    
    Page<Calendar> findAllByIsDeletedFalse(Pageable pageable);
    
    Optional<Calendar> findCalendarByIdAndIsDeletedFalse(String id);
    
    Optional<Calendar> findCalendarByIdAndIsDeletedTrue(String id);
    
    Optional<Calendar> findCalendarByUserIdAndTitleAndIsDeletedFalse(String userId, String title);
    
    @Query("SELECT c FROM Calendar c WHERE c.userId = :userId AND c.isPrimary = true AND c.isDeleted = false")
    Optional<Calendar> findPrimaryCalendarByUserId(@Param("userId") String userId);
    
    @Query("SELECT c FROM Calendar c WHERE c.id IN :ids AND c.isDeleted = false")
    List<Calendar> findAllByIdInAndIsDeletedFalse(@Param("ids") List<String> ids);
    
    @Query("SELECT c FROM Calendar c WHERE c.userId = :userId AND c.isDeleted = false AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Calendar> searchCalendars(@Param("userId") String userId, 
                                  @Param("query") String query, 
                                  Pageable pageable);
    
    // Legacy method for backward compatibility
    default List<Calendar> findCalendarByUserId(String userId) {
        return findCalendarByUserIdAndIsDeletedFalse(userId);
    }
}
