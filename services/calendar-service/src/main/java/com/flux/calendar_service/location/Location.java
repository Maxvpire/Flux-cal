package com.flux.calendar_service.location;

import com.flux.calendar_service.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "locations")
@EntityListeners(AuditingEntityListener.class)
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "room")
    private String room;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "place_id")
    private String placeId;

    @OneToMany(mappedBy = "location")
    private Set<Event> events = new HashSet<>();
}
