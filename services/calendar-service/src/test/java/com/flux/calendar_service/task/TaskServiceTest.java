package com.flux.calendar_service.task;

import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id("evt-1")
                .tasks(new ArrayList<>())
                .build();

        task = Task.builder()
                .id("tsk-1")
                .task("Do this")
                .isDone(false)
                .event(event)
                .build();
    }

    @Test
    void createTask_Success() {
        // Arrange
        when(eventRepository.findById("evt-1")).thenReturn(Optional.of(event));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        String id = taskService.createTask("evt-1", "Do this");

        // Assert
        assertEquals("tsk-1", id);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void completeTask_Success() {
        // Arrange
        when(taskRepository.findById("tsk-1")).thenReturn(Optional.of(task));

        // Act
        taskService.completeTask("tsk-1");

        // Assert
        assertTrue(task.isDone());
        verify(taskRepository).save(task);
    }

    @Test
    void incompleteTask_Success() {
        // Arrange
        task.setDone(true);
        when(taskRepository.findById("tsk-1")).thenReturn(Optional.of(task));

        // Act
        taskService.incompleteTask("tsk-1");

        // Assert
        assertFalse(task.isDone());
        verify(taskRepository).save(task);
    }

    @Test
    void getAllTasksByEventId_Success() {
        // Arrange
        when(taskRepository.findAllByEventId("evt-1")).thenReturn(List.of(task));
        when(taskMapper.toTaskResponse(task)).thenReturn(new TaskResponse("tsk-1", "Do this", false));

        // Act
        var results = taskService.getAllTasksByEventId("evt-1");

        // Assert
        assertFalse(results.isEmpty());
        assertEquals("tsk-1", results.get(0).id());
    }

    @Test
    void updateTask_Success() {
        // Arrange
        TaskRequest request = new TaskRequest("Updated", true);
        when(taskRepository.findById("tsk-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toTaskResponse(task)).thenReturn(new TaskResponse("tsk-1", "Updated", true));

        // Act
        TaskResponse response = taskService.updateTask("tsk-1", request);

        // Assert
        assertNotNull(response);
    }

    @Test
    void deleteTask_Success() {
        // Arrange
        when(taskRepository.findById("tsk-1")).thenReturn(Optional.of(task));

        // Act
        taskService.deleteTask("tsk-1");

        // Assert
        verify(taskRepository).delete(task);
    }

    @Test
    void getById_NotFound() {
        when(taskRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> taskService.getById("invalid"));
    }
}
