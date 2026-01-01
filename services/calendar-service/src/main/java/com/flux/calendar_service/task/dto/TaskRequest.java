package com.flux.calendar_service.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
        @NotBlank(message = "Task description is required") String task,
        boolean isDone) {
}
