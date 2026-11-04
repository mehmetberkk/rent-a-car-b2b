package com.rentacar.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> {
                    log.error("Sending failed record to DLT: topic={}, key={}, error={}",
                            record.topic(), record.key(), ex.getMessage());
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for record on topic={}, error={}",
                        deliveryAttempt, record.topic(), ex.getMessage()));

        return handler;
    }
}

