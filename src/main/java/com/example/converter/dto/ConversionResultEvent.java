package com.example.converter.dto;

public record ConversionResultEvent(
        String eventId,
        String sourceBucket,
        String sourceKey,
        String resultBucket,
        String resultKey
) {}
