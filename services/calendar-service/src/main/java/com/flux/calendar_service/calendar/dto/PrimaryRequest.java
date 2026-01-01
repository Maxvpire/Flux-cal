package com.flux.calendar_service.calendar.dto;

import jakarta.validation.constraints.NotBlank;

public record PrimaryRequest (
    @NotBlank
    String userId
){}
