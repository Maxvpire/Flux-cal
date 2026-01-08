package com.flux.calendar_service.attachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flux.calendar_service.attachment.dto.AttachmentResponse;
import com.flux.calendar_service.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private AttachmentController attachmentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController).build();
    }

    @Test
    void uploadAttachment_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        AttachmentResponse response = new AttachmentResponse("att-1", "http://url", "test.txt", "text/plain", 7L, LocalDateTime.now(), null);
        
        when(attachmentService.uploadAttachment(eq("evt-1"), any())).thenReturn(response);

        mockMvc.perform(multipart("/attachments/event/evt-1")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("att-1"));
    }

    @Test
    void getFile_Success() throws Exception {
        Attachment attachment = Attachment.builder()
                .id("att-1")
                .title("test.txt")
                .mimeType("text/plain")
                .fileUrl("http://url")
                .build();
        
        when(attachmentService.getAttachment("att-1")).thenReturn(attachment);
        when(attachmentService.downloadAttachment("att-1")).thenReturn(new ByteArrayInputStream("content".getBytes()));

        mockMvc.perform(get("/attachments/att-1/file"))
                .andExpect(status().isOk())
                .andExpect(content().string("content"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    void deleteAttachment_Success() throws Exception {
        mockMvc.perform(delete("/attachments/att-1"))
                .andExpect(status().isNoContent());

        verify(attachmentService).deleteAttachment("att-1");
    }
}
