package com.flux.calendar_service.task.dto;

public record TaskResponse(
        String id,
        String task,
        boolean isDone) {
}
