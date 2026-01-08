package com.flux.calendar_service.attachment;

import com.flux.calendar_service.attachment.dto.AttachmentResponse;
import com.flux.calendar_service.event.Event;
import com.flux.calendar_service.event.EventRepository;
import com.flux.calendar_service.storage.MinioService;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private MinioService minioService;

    @InjectMocks
    private AttachmentService attachmentService;

    private Attachment attachment;
    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id("evt-1")
                .attachments(new ArrayList<>())
                .build();

        attachment = Attachment.builder()
                .id("att-1")
                .title("file.txt")
                .fileUrl("http://minio/bucket/file.txt")
                .event(event)
                .build();
    }

    @Test
    void uploadAttachment_Success() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", "content".getBytes());
        when(eventRepository.findById("evt-1")).thenReturn(Optional.of(event));
        when(minioService.uploadFile(file)).thenReturn("file.txt");
        when(minioService.getFileUrl("file.txt")).thenReturn("http://minio/bucket/file.txt");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);
        
        AttachmentResponse response = new AttachmentResponse("att-1", "http://minio/bucket/file.txt", "file.txt", "text/plain", 7L, LocalDateTime.now(), null);
        when(attachmentMapper.toAttachmentResponse(attachment)).thenReturn(response);

        // Act
        AttachmentResponse result = attachmentService.uploadAttachment("evt-1", file);

        // Assert
        assertEquals("att-1", result.id());
        verify(minioService).uploadFile(file);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void downloadAttachment_Success() {
        // Arrange
        when(attachmentRepository.findById("att-1")).thenReturn(Optional.of(attachment));
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        when(minioService.getFile("file.txt")).thenReturn(inputStream);

        // Act
        InputStream result = attachmentService.downloadAttachment("att-1");

        // Assert
        assertNotNull(result);
    }

    @Test
    void deleteAttachment_Success() {
        // Arrange
        when(attachmentRepository.findById("att-1")).thenReturn(Optional.of(attachment));

        // Act
        attachmentService.deleteAttachment("att-1");

        // Assert
        verify(minioService).deleteFile("file.txt");
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void getAttachment_NotFound() {
        when(attachmentRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> 
            attachmentService.getAttachment("invalid")
        );
    }
}
