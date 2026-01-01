package com.flux.calendar_service.attachment;

import com.flux.calendar_service.attachment.dto.AttachmentResponse;
import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.storage.MinioService;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final EventRepository eventRepository;
    private final AttachmentMapper attachmentMapper;
    private final MinioService minioService;

    public Attachment getAttachment(String attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found with ID: " + attachmentId));
    }

    public InputStream downloadAttachment(String attachmentId) {
        Attachment attachment = getAttachment(attachmentId);
        String fileUrl = attachment.getFileUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        return minioService.getFile(fileName);
    }

    @Transactional
    public AttachmentResponse uploadAttachment(String eventId, org.springframework.web.multipart.MultipartFile file) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with ID: " + eventId));

        String fileName = minioService.uploadFile(file);
        String fileUrl = minioService.getFileUrl(fileName);

        Attachment attachment = Attachment.builder()
                .title(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .event(event)
                .build();

        Attachment savedAttachment = attachmentRepository.save(attachment);
        return attachmentMapper.toAttachmentResponse(savedAttachment);
    }

    @Transactional
    public void deleteAttachment(String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found with ID: " + attachmentId));

        // Remove from Event's list to maintain bidirectional consistency
        if (attachment.getEvent() != null) {
            attachment.getEvent().getAttachments().remove(attachment);
        }

        // Extract fileName from fileUrl
        String fileUrl = attachment.getFileUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // Delete from MinIO
        minioService.deleteFile(fileName);

        // Delete from DB
        attachmentRepository.delete(attachment);
    }
}
