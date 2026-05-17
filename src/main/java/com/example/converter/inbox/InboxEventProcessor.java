package com.example.converter.inbox;

import com.example.converter.dto.ConversionRequestEvent;
import com.example.converter.dto.ConversionResultEvent;
import com.example.converter.inbox.entity.InboxEvent;
import com.example.converter.inbox.entity.InboxEventStatus;
import com.example.converter.inbox.repository.InboxEventRepository;
import com.example.converter.kafka.KafkaProducer;
import com.example.converter.service.ConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final ConversionService conversionService;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${inbox.scheduler.fixed-delay-ms}")
    public void processPendingEvents() {
        List<InboxEvent> pending = inboxEventRepository.findAllByStatus(InboxEventStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        log.info("Processing {} pending inbox event(s)", pending.size());
        pending.forEach(this::processEvent);
    }

    private void processEvent(InboxEvent event) {
        try {
            ConversionRequestEvent request = objectMapper.readValue(event.getPayload(), ConversionRequestEvent.class);
            ConversionResultEvent result = conversionService.convert(request);
            kafkaProducer.sendResult(result);
        } catch (Exception e) {
            log.error("Failed to process inbox event: eventId={}", event.getEventId(), e);
            markFailed(event);
            return;
        }

        try {
            markProcessed(event);
        } catch (Exception e) {
            log.error("Failed to mark event as processed, will retry: eventId={}", event.getEventId(), e);
        }
    }

    private void markProcessed(InboxEvent event) {
        event.setStatus(InboxEventStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        inboxEventRepository.save(event);
    }

    private void markFailed(InboxEvent event) {
        event.setStatus(InboxEventStatus.FAILED);
        event.setProcessedAt(LocalDateTime.now());
        inboxEventRepository.save(event);
    }
}
