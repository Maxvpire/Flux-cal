package com.flux.calendar_service.attachment;

import com.flux.calendar_service.attachment.dto.AttachmentRequest;
import com.flux.calendar_service.attachment.dto.AttachmentResponse;
import com.flux.calendar_service.event.Event;
import org.springframework.stereotype.Service;

@Service
public class AttachmentMapper {

    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileUrl(),
                attachment.getTitle(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getCreatedAt(),
                attachment.getUpdatedAt());
    }

    public Attachment toAttachment(AttachmentRequest request, Event event) {
        if (request == null) {
            return null;
        }
        return Attachment.builder()
                .fileUrl(request.fileUrl())
                .title(request.title())
                .mimeType(request.mimeType())
                .fileSize(request.fileSize())
                .event(event)
                .build();
    }
}
