package com.example.converter.consumer;

import com.example.converter.dto.ConversionRequestEvent;
import com.example.converter.inbox.entity.InboxEvent;
import com.example.converter.inbox.repository.InboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversionRequestConsumer {

    private final InboxEventRepository inboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.conversion-request}")
    public void consume(ConversionRequestEvent event, Acknowledgment acknowledgment) {
        log.info("Received conversion request: eventId={}, bucket={}, key={}",
                event.eventId(), event.bucket(), event.objectKey());

        try {
            String payload = objectMapper.writeValueAsString(event);
            inboxEventRepository.save(InboxEvent.pending(event.eventId(), payload));
            log.info("Event saved to inbox: {}", event.eventId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event ignored: {}", event.eventId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event: " + event.eventId(), e);
        }

        acknowledgment.acknowledge();
    }
}
