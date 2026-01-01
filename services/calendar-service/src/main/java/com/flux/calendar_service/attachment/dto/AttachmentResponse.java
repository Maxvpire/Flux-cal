package com.flux.calendar_service.attachment.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(
        String id,
        String fileUrl,
        String title,
        String mimeType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
