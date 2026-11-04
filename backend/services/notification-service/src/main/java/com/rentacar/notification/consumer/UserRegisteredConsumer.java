package com.rentacar.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.identity.UserRegisteredPayload;
import com.rentacar.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final Set<UUID> processedEvents = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "identity.user.registered", groupId = "notification-service-group")
    public void consumeUserRegistered(String message) {
        try {
            EventEnvelope<UserRegisteredPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<>() {}
            );

            if (!processedEvents.add(envelope.eventId())) {
                log.info("Duplicate user.registered event ignored: {}", envelope.eventId());
                return;
            }

            UserRegisteredPayload payload = envelope.payload();
            log.info("Processing user registered event: email={}, eventId={}", payload.email(), envelope.eventId());
            emailService.sendActivationEmail(payload.email(), payload.firstName(), payload.activationToken());
        } catch (Exception e) {
            log.error("Failed to process user registered event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing user registered event", e);
        }
    }
}

