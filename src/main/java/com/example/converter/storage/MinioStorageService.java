package com.example.converter.storage;

import com.example.converter.config.MinioProperties;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @PostConstruct
    public void ensureBucketsExist() {
        createBucketIfAbsent(minioProperties.sourceBucket());
        createBucketIfAbsent(minioProperties.resultBucket());
    }

    private void createBucketIfAbsent(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket exists: " + bucket, e);
        }
    }

    public byte[] downloadFile(String bucket, String objectKey) {
        log.info("Downloading file: bucket={}, key={}", bucket, objectKey);
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectKey, e);
        }
    }

    public void uploadFile(String bucket, String objectKey, byte[] content, String contentType) {
        log.info("Uploading file: bucket={}, key={}, size={}", bucket, objectKey, content.length);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + objectKey, e);
        }
    }

    public String getSourceBucket() {
        return minioProperties.sourceBucket();
    }

    public String getResultBucket() {
        return minioProperties.resultBucket();
    }
}
