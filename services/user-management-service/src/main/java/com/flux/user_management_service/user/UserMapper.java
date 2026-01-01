package com.flux.user_management_service.user;

import org.springframework.stereotype.Service;

import com.flux.user_management_service.user.dto.UserResponse;

@Service
public class UserMapper {
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstname(),
            user.getLastname(),
            user.getEmail(),
            user.getBirthday()
        );
    }
}
