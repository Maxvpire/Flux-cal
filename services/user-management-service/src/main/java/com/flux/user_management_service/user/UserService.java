package com.flux.user_management_service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.flux.user_management_service.exception.UserAlreadyExistsException;
import com.flux.user_management_service.exception.UserNotFoundException;
import com.flux.user_management_service.user.dto.UserRequest;
import com.flux.user_management_service.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @CachePut(value = "users", key = "#result")
    public String createUser(String keycloakId, UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }
        User user = User.builder()
                .keycloakId(keycloakId)
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .birthday(request.birthday())
                .build();

        return userRepository.save(user).getId();
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(String id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    @Cacheable(value = "users")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateUser(String id, UserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        mergeUser(user, request);

        userRepository.save(user);
    }

    private void mergeUser(User user, UserRequest request) {
        if(StringUtils.isNotBlank(request.firstname())){
            user.setFirstname(request.firstname());
        }
        if(StringUtils.isNotBlank(request.lastname())){
            user.setLastname(request.lastname());
        }
    }

    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found!");
        }
        userRepository.deleteById(id);
    }
}
