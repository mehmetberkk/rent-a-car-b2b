package com.rentacar.identity.event;

import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.identity.UserRegisteredPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventProducer {

    public static final String USER_REGISTERED_TOPIC = "identity.user.registered";
    public static final String AGENCY_UPSERTED_TOPIC = "identity.agency.upserted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredPayload payload, UUID correlationId) {
        EventEnvelope<UserRegisteredPayload> envelope = EventEnvelope.of(
                USER_REGISTERED_TOPIC,
                correlationId,
                payload
        );
        log.info("Publishing user registered event for email: {}, eventId: {}", payload.email(), envelope.eventId());
        kafkaTemplate.send(USER_REGISTERED_TOPIC, payload.userId().toString(), envelope);
    }

    public void publishAgencyUpserted(Object payload, UUID agencyId, UUID correlationId) {
        EventEnvelope<Object> envelope = EventEnvelope.of(
                AGENCY_UPSERTED_TOPIC,
                correlationId,
                payload
        );
        log.info("Publishing agency upserted event for agencyId: {}, eventId: {}", agencyId, envelope.eventId());
        kafkaTemplate.send(AGENCY_UPSERTED_TOPIC, agencyId.toString(), envelope);
    }
}

