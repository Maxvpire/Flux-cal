
package com.flux.user_management_service.user.dto;

import java.time.LocalDate;

public record UserResponse(
        String id,
        String firstname,
        String lastname,
        String email,
        LocalDate birthday) implements java.io.Serializable {

}
