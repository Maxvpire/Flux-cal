package com.flux.calendar_service.location;

import com.flux.calendar_service.location.dto.LocationRequest;
import com.flux.calendar_service.location.dto.LocationResponse;
import com.flux.calendar_service.location.dto.OpenInMapResponse;
import com.flux.calendar_service.location.dto.UpdateLocation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event/location")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/{id}")
    public ResponseEntity<String> createLocation(
            @PathVariable("id") String id,
            @RequestBody @Valid LocationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(locationService.addLocation(id, request));
    }

    @GetMapping("/maps/{id}")
    public ResponseEntity<OpenInMapResponse> openInMaps(@PathVariable String id){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.openInMaps(id));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<LocationResponse> findById(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody UpdateLocation request) {
        locationService.updateLocation(id, request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}
