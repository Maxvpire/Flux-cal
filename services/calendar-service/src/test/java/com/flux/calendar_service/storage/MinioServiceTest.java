package com.flux.calendar_service.storage;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioService minioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
    }

    @Test
    void init_BucketExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.init();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void init_BucketDoesNotExist() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.init();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void uploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        
        // Mock putObject to do nothing (void) or return null via generic Object
        // But putObject returns ObjectWriteResponse.
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(new ObjectWriteResponse(null, "bucket", "region", "object", "etag", null));

        String fileName = minioService.uploadFile(file);

        assertNotNull(fileName);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getFile_Success() throws Exception {
        InputStream stream = new ByteArrayInputStream("content".getBytes());
        GetObjectResponse response = new GetObjectResponse(null, "bucket", "region", "object", stream);
        
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        InputStream result = minioService.getFile("test.txt");

        assertNotNull(result);
    }

    @Test
    void deleteFile_Success() throws Exception {
        minioService.deleteFile("test.txt");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
