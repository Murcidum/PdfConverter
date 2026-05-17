package com.example.converter.dto;

public record ConversionRequestEvent(
        String eventId,
        String bucket,
        String objectKey
) {}
