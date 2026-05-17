package com.example.converter.kafka;

import com.example.converter.dto.ConversionResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, ConversionResultEvent> kafkaTemplate;

    @Value("${kafka.topics.conversion-result}")
    private String resultTopic;

    public void sendResult(ConversionResultEvent event) {
        log.info("Sending result event: eventId={}, resultKey={}", event.eventId(), event.resultKey());
        try {
            kafkaTemplate.send(resultTopic, event.eventId(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send result event to Kafka: " + event.eventId(), e);
        }
    }
}
