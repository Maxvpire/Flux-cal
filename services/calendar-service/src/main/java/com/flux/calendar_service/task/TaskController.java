package com.flux.calendar_service.task;

import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Controller", description = "Endpoints for managing tasks")
public class TaskController {
    private final TaskService taskService;

    @Operation(summary = "Create task", description = "Creates a new task associated with an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task created successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PostMapping("/event/{eventId}")
    public ResponseEntity<String> createTask(
            @PathVariable String eventId,
            @RequestBody String name) {
        return ResponseEntity.ok(taskService.createTask(eventId, name));
    }

    @Operation(summary = "Get tasks by event", description = "Retrieves all tasks for a specific event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tasks"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/event/{eventId}")
    public ResponseEntity<java.util.List<TaskResponse>> getTasksByEventId(@PathVariable String eventId) {
        return ResponseEntity.ok(taskService.getAllTasksByEventId(eventId));
    }

    @Operation(summary = "Get all tasks", description = "Retrieves a list of all tasks")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    public ResponseEntity<java.util.List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @Operation(summary = "Get task by ID", description = "Retrieves a specific task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved task"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getById(id));
    }

    @Operation(summary = "Toggle task status", description = "Toggles the completion status of a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Task status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
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

    @Operation(summary = "Update task", description = "Updates an existing task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String id,
            @RequestBody @Valid TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @Operation(summary = "Delete task", description = "Deletes a task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
