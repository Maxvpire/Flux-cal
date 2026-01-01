package com.flux.calendar_service.task;

import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping("/event/{eventId}")
    public ResponseEntity<String> createTask(
            @PathVariable String eventId,
            @RequestBody String name) {
        return ResponseEntity.ok(taskService.createTask(eventId, name));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<java.util.List<TaskResponse>> getTasksByEventId(@PathVariable String eventId) {
        return ResponseEntity.ok(taskService.getAllTasksByEventId(eventId));
    }

    @GetMapping
    public ResponseEntity<java.util.List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleTask(@PathVariable String id) {
        TaskResponse task = taskService.getById(id);
        if (task.isDone()) {
            taskService.incompleteTask(id);
        } else {
            taskService.completeTask(id);
        }
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String id,
            @RequestBody @Valid TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
