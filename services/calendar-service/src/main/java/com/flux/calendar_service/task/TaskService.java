package com.flux.calendar_service.task;

import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public String createTask(String eventId, String taskDescription) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        Task task = Task.builder()
                .task(taskDescription)
                .isDone(false)
                .event(event)
                .build();

        return taskRepository.save(task).getId();
    }

    public List<TaskResponse> getAllTasksByEventId(String eventId) {
        return taskRepository.findAllByEventId(eventId).stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getById(String id) {
        return taskRepository.findById(id)
                .map(taskMapper::toTaskResponse)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + id));
    }

    @Transactional
    public void completeTask(String id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + id));
        task.setDone(true);
        taskRepository.save(task);
    }

    @Transactional
    public void incompleteTask(String id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + id));
        task.setDone(false);
        taskRepository.save(task);
    }

    @Transactional
    public TaskResponse updateTask(String id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + id));

        task.setTask(request.task());
        task.setDone(request.isDone());

        return taskMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(String id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found with ID: " + id));

        // Remove from Event's list to maintain bidirectional consistency
        if (task.getEvent() != null) {
            task.getEvent().getTasks().remove(task);
        }

        taskRepository.delete(task);
    }
}
