package com.rentacar.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.fleet.VehicleComplianceAlertPayload;
import com.rentacar.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceAlertConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final Set<UUID> processedEvents = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "fleet.vehicle.compliance-alert", groupId = "notification-service-compliance-group")
    public void consumeComplianceAlert(String message) {
        try {
            EventEnvelope<VehicleComplianceAlertPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<>() {}
            );

            if (!processedEvents.add(envelope.eventId())) {
                log.info("Duplicate fleet.vehicle.compliance-alert event ignored: {}", envelope.eventId());
                return;
            }

            VehicleComplianceAlertPayload payload = envelope.payload();
            log.info("Processing vehicle compliance alert: vehicleId={}, plate={}, alertsCount={}",
                    payload.vehicleId(), payload.plate(), payload.alerts().size());

            String alertSummary = payload.alerts().stream()
                    .map(item -> String.format("• <strong>%s</strong>: Bitiş Tarihi: %s (Kalan Gün: %d, Durum: %s)",
                            translateType(item.type()),
                            item.expiryDate(),
                            item.daysRemaining(),
                            item.severity()))
                    .collect(Collectors.joining("<br/>"));

            String recipientEmail = "agency-" + payload.agencyId() + "@rentacar-b2b.com";
            emailService.sendComplianceAlertEmail(recipientEmail, payload.plate(), alertSummary);
            log.info("Compliance alert notification processed successfully for plate={}", payload.plate());
        } catch (Exception e) {
            log.error("Failed to process fleet.vehicle.compliance-alert event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing vehicle compliance alert", e);
        }
    }

    private String translateType(String type) {
        return switch (type) {
            case "TRAFFIC_INSURANCE" -> "Trafik Sigortası";
            case "CASCO" -> "Kasko Sigortası";
            case "INSPECTION" -> "Araç Muayenesi";
            default -> type;
        };
    }
}

