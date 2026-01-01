package com.flux.user_management_service.user.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
                @Schema(description = "First name of the user", example = "John") @NotEmpty @NotBlank String firstname,

                @Schema(description = "Last name of the user", example = "Doe") @NotEmpty @NotBlank String lastname,

                @Schema(description = "Email address of the user", example = "john.doe@example.com") @NotEmpty @NotBlank @Email String email,

                @Schema(description = "Birth date of the user", example = "1990-01-01") @NotNull LocalDate birthday) {

}
