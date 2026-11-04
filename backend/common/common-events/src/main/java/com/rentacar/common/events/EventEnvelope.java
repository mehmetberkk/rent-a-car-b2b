package com.rentacar.common.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        UUID correlationId,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, UUID correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                correlationId != null ? correlationId : UUID.randomUUID(),
                payload
        );
    }
}

