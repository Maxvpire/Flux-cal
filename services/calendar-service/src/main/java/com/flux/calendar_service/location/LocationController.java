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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@RequestMapping("/event/location")
@RequiredArgsConstructor
@Tag(name = "Location Controller", description = "Endpoints for managing event locations")
public class LocationController {
    private final LocationService locationService;

    @Operation(summary = "Create location", description = "Creates a new location for a specific event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Location created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PostMapping("/{id}")
    public ResponseEntity<String> createLocation(
            @PathVariable("id") String id,
            @RequestBody @Valid LocationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(locationService.addLocation(id, request));
    }

    @Operation(summary = "Open in Maps", description = "Generates a maps link (e.g., Google Maps) for the location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully generated map link"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @GetMapping("/maps/{id}")
    public ResponseEntity<OpenInMapResponse> openInMaps(@PathVariable String id){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.openInMaps(id));
    }

    @Operation(summary = "Get location by ID", description = "Retrieves a specific location by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved location"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @GetMapping("/id/{id}")
    public ResponseEntity<LocationResponse> findById(@PathVariable String id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.findById(id));
    }

    @Operation(summary = "Get all locations", description = "Retrieves a list of all locations")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(locationService.findAll());
    }

    @Operation(summary = "Update location", description = "Updates an existing location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Location updated successfully"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody UpdateLocation request) {
        locationService.updateLocation(id, request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @Operation(summary = "Delete location", description = "Deletes a location by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Location deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }
}
