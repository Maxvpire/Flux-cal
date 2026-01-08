package com.flux.calendar_service.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.flux.calendar_service.exceptions.MinIoBucketInitializingErrorException;
import com.flux.calendar_service.exceptions.MinIoDeleteErrorException;
import com.flux.calendar_service.exceptions.MinIoRetrievingErrorException;
import com.flux.calendar_service.exceptions.MinIoUploadingErrorException;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new MinIoBucketInitializingErrorException("Error initializing MinIO bucket" + e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file) {
        String fileName = (System.currentTimeMillis() + "_" + file.getOriginalFilename()).replace(" ", "");
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return fileName;
        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new MinIoUploadingErrorException("Failed to upload file: " + e.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        return String.format("%s/%s/%s", "https://flux-minios3.onrender.com", bucketName, fileName);
    }

    public InputStream getFile(String fileName) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Error retrieving file from MinIO: {}", fileName, e);
            throw new MinIoRetrievingErrorException("Failed to retrieve file" + e.getMessage());
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", fileName, e);
            throw new MinIoDeleteErrorException("Failed to delete file" + e.getMessage()    );
        }
    }
}
