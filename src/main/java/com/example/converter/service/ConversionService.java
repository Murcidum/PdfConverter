package com.example.converter.service;

import com.example.converter.converter.FileConverter;
import com.example.converter.dto.ConversionRequestEvent;
import com.example.converter.dto.ConversionResultEvent;
import com.example.converter.storage.MinioStorageService;
import com.example.converter.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final MinioStorageService minioStorageService;
    private final Map<String, FileConverter> fileConverters;

    public ConversionResultEvent convert(ConversionRequestEvent request) {
        String objectKey = request.objectKey();
        String ext = FileUtils.extractExtension(objectKey);

        FileConverter converter = fileConverters.get(ext);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported file format: " + ext);
        }

        log.info("Converting file: bucket={}, key={}, format={}", request.bucket(), objectKey, ext);

        byte[] sourceBytes = minioStorageService.downloadFile(request.bucket(), objectKey);
        byte[] pdfBytes = converter.convert(sourceBytes);

        String resultKey = toResultKey(objectKey);
        String resultBucket = minioStorageService.getResultBucket();
        minioStorageService.uploadFile(resultBucket, resultKey, pdfBytes, "application/pdf");

        log.info("Conversion done: resultBucket={}, resultKey={}", resultBucket, resultKey);

        return new ConversionResultEvent(
                request.eventId(),
                request.bucket(),
                objectKey,
                resultBucket,
                resultKey,
                null
        );
    }

    private String toResultKey(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        String base = dot >= 0 ? objectKey.substring(0, dot) : objectKey;
        return base + ".pdf";
    }
}
