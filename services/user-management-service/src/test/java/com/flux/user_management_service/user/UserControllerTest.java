package com.flux.user_management_service.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flux.user_management_service.user.dto.UserRequest;
import com.flux.user_management_service.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new com.flux.user_management_service.handler.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userRequest = new UserRequest("John", "Doe", "john.doe@example.com", LocalDate.of(1990, 1, 1));
        userResponse = new UserResponse("1", "John", "Doe", "john.doe@example.com", LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_ShouldReturnUserId() throws Exception {
        when(userService.createUser(eq("keycloak-1"), any(UserRequest.class))).thenReturn("1");

        mockMvc.perform(post("/users")
                .param("keycloakId", "keycloak-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value("1"));
    }

    @Test
    void getUserById_ShouldReturnUserResponse() throws Exception {
        when(userService.getUserById("1")).thenReturn(userResponse);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.firstname").value("John"));
    }

    @Test
    void getAllUsers_ShouldReturnListOfUserResponses() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void updateUser_ShouldUpdateUser() throws Exception {
        mockMvc.perform(put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq("1"), any(UserRequest.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isAccepted());

        verify(userService).deleteUser("1");
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        UserRequest invalidRequest = new UserRequest("", "", "invalid-email", null);

        mockMvc.perform(post("/users")
                .param("keycloakId", "keycloak-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstname").exists())
                .andExpect(jsonPath("$.errors.lastname").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.birthday").exists());
    }
}
