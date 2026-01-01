package com.flux.calendar_service.attachment;

import com.flux.calendar_service.attachment.dto.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping("/event/{eventId}")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable String eventId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(attachmentService.uploadAttachment(eventId, file));
    }

    @GetMapping("/{attachmentId}/file")
    public ResponseEntity<Resource> getFile(@PathVariable String attachmentId) {
        Attachment attachment = attachmentService.getAttachment(attachmentId);
        java.io.InputStream inputStream = attachmentService.downloadAttachment(attachmentId);
        Resource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getTitle() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable String attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
