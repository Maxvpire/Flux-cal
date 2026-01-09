package com.flux.calendar_service.location;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, String> {
    Optional<Location> findByEventId(String eventId);
    
    Page<Location> findByCityContainingIgnoreCase(String city, Pageable pageable);
    
    Page<Location> findByCountryContainingIgnoreCase(String country, Pageable pageable);
    
    @Query("SELECT l FROM Location l WHERE " +
           "LOWER(l.placeName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.streetAddress) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.country) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Location> searchLocations(@Param("query") String query, Pageable pageable);
    
    @Query(value = "SELECT * FROM locations l WHERE " +
           "ST_Distance_Sphere(point(l.longitude, l.latitude), point(:longitude, :latitude)) <= :radius * 1000",
           nativeQuery = true)
    Page<Location> findNearbyLocations(@Param("latitude") Double latitude,
                                      @Param("longitude") Double longitude,
                                      @Param("radius") Double radius,
                                      Pageable pageable);
}
