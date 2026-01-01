package com.flux.user_management_service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flux.user_management_service.user.dto.UserRequest;
import com.flux.user_management_service.user.dto.UserResponse;

import com.flux.user_management_service.exception.UserAlreadyExistsException;
import com.flux.user_management_service.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("1")
                .keycloakId("keycloak-1")
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        userRequest = new UserRequest("John", "Doe", "john.doe@example.com", LocalDate.of(1990, 1, 1));
        userResponse = new UserResponse("1", "John", "Doe", "john.doe@example.com", LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_ShouldReturnUserId_WhenEmailDoesNotExist() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        String userId = userService.createUser("keycloak-1", userRequest);

        assertEquals("1", userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser("keycloak-1", userRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_ShouldReturnUserResponse_WhenUserExists() {
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse response = userService.getUserById("1");

        assertNotNull(response);
        assertEquals("1", response.id());
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserDoesNotExist() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById("1"));
    }

    @Test
    void getAllUsers_ShouldReturnListOfUserResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(1, responses.size());
        assertEquals("1", responses.get(0).id());
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenUserExists() {
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        userService.updateUser("1", userRequest);

        verify(userRepository).save(user);
        assertEquals("John", user.getFirstname());
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserDoesNotExist() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser("1", userRequest));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.existsById("1")).thenReturn(true);

        userService.deleteUser("1");

        verify(userRepository).deleteById("1");
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserDoesNotExist() {
        when(userRepository.existsById("1")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("1"));
    }
}
