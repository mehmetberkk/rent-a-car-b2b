package com.rentacar.fleet.event;

import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.fleet.VehicleComplianceAlertPayload;
import com.rentacar.common.events.fleet.VehicleStatusChangedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleEventProducer {

    public static final String VEHICLE_STATUS_CHANGED_TOPIC = "fleet.vehicle.status-changed";
    public static final String COMPLIANCE_ALERT_TOPIC = "fleet.vehicle.compliance-alert";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStatusChanged(VehicleStatusChangedPayload payload, UUID correlationId) {
        EventEnvelope<VehicleStatusChangedPayload> envelope = EventEnvelope.of(
                VEHICLE_STATUS_CHANGED_TOPIC,
                correlationId,
                payload
        );
        log.info("Publishing vehicle status changed event: vehicleId={}, newStatus={}, eventId={}",
                payload.vehicleId(), payload.newStatus(), envelope.eventId());
        kafkaTemplate.send(VEHICLE_STATUS_CHANGED_TOPIC, payload.vehicleId().toString(), envelope);
    }

    public void publishComplianceAlert(VehicleComplianceAlertPayload payload, UUID correlationId) {
        EventEnvelope<VehicleComplianceAlertPayload> envelope = EventEnvelope.of(
                COMPLIANCE_ALERT_TOPIC,
                correlationId,
                payload
        );
        log.info("Publishing compliance alert event: vehicleId={}, plate={}, alertsCount={}",
                payload.vehicleId(), payload.plate(), payload.alerts().size());
        kafkaTemplate.send(COMPLIANCE_ALERT_TOPIC, payload.vehicleId().toString(), envelope);
    }
}

