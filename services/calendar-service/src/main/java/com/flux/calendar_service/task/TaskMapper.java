package com.flux.calendar_service.task;

import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import org.springframework.stereotype.Service;

@Service
public class TaskMapper {

    public TaskResponse toTaskResponse(Task task) {
        if (task == null) {
            return null;
        }
        return new TaskResponse(
                task.getId(),
                task.getTask(),
                task.isDone());
    }

    public Task toTask(TaskRequest request, Event event) {
        if (request == null) {
            return null;
        }
        return Task.builder()
                .task(request.task())
                .isDone(request.isDone())
                .event(event)
                .build();
    }
}
