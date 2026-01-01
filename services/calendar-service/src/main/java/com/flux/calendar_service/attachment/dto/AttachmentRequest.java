package com.flux.calendar_service.attachment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttachmentRequest(
        @NotBlank String fileUrl,
        @NotBlank String title,
        @NotBlank String mimeType,
        @NotNull Long fileSize) {
}
