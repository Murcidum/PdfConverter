package com.example.converter.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        log.error("Skipping unprocessable record at offset={} partition={}: {}",
                                record.offset(), record.partition(), ex.getMessage()),
                new FixedBackOff(0L, 0L)
        );
        handler.addNotRetryableExceptions(org.springframework.kafka.support.serializer.DeserializationException.class);
        return handler;
    }
}
