package com.flux.calendar_service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flux.calendar_service.task.dto.TaskRequest;
import com.flux.calendar_service.task.dto.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createTask_Success() throws Exception {
        when(taskService.createTask("evt-1", "Task Name")).thenReturn("tsk-1");

        mockMvc.perform(post("/tasks/event/evt-1")
                        .content("Task Name"))
                .andExpect(status().isOk())
                .andExpect(content().string("tsk-1"));
    }

    @Test
    void getTasksByEventId_Success() throws Exception {
        when(taskService.getAllTasksByEventId("evt-1")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tasks/event/evt-1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getTaskById_Success() throws Exception {
        TaskResponse response = new TaskResponse("tsk-1", "Name", false);
        when(taskService.getById("tsk-1")).thenReturn(response);

        mockMvc.perform(get("/tasks/tsk-1"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleTask_Complete_Success() throws Exception {
        TaskResponse response = new TaskResponse("tsk-1", "Name", false);
        when(taskService.getById("tsk-1")).thenReturn(response);

        mockMvc.perform(patch("/tasks/tsk-1/toggle"))
                .andExpect(status().isAccepted());

        verify(taskService).completeTask("tsk-1");
    }

    @Test
    void toggleTask_Incomplete_Success() throws Exception {
        TaskResponse response = new TaskResponse("tsk-1", "Name", true);
        when(taskService.getById("tsk-1")).thenReturn(response);

        mockMvc.perform(patch("/tasks/tsk-1/toggle"))
                .andExpect(status().isAccepted());

        verify(taskService).incompleteTask("tsk-1");
    }

    @Test
    void updateTask_Success() throws Exception {
        TaskRequest request = new TaskRequest("Updated", true);
        when(taskService.updateTask(eq("tsk-1"), any(TaskRequest.class))).thenReturn(new TaskResponse("tsk-1", "Updated", true));

        mockMvc.perform(put("/tasks/tsk-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTask_Success() throws Exception {
        mockMvc.perform(delete("/tasks/tsk-1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask("tsk-1");
    }
}
