package com.rentacar.pool.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.fleet.VehicleStatusChangedPayload;
import com.rentacar.pool.model.AgencyProjection;
import com.rentacar.pool.model.PoolListing;
import com.rentacar.pool.model.ProcessedEvent;
import com.rentacar.pool.repository.AgencyProjectionRepository;
import com.rentacar.pool.repository.PoolListingRepository;
import com.rentacar.pool.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleStatusConsumer {

    private final PoolListingRepository poolListingRepository;
    private final AgencyProjectionRepository agencyProjectionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "fleet.vehicle.status-changed", groupId = "pool-service-vehicle-status-group")
    public void consumeVehicleStatusChanged(String message) {
        try {
            EventEnvelope<VehicleStatusChangedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<>() {}
            );

            if (processedEventRepository.existsById(envelope.eventId())) {
                log.info("Duplicate fleet.vehicle.status-changed event ignored: {}", envelope.eventId());
                return;
            }

            VehicleStatusChangedPayload payload = envelope.payload();
            log.info("Processing vehicle status changed: vehicleId={}, status={}, previousStatus={}",
                    payload.vehicleId(), payload.newStatus(), payload.previousStatus());

            if ("IN_POOL".equalsIgnoreCase(payload.newStatus())) {
                Optional<AgencyProjection> agencyOpt = agencyProjectionRepository.findById(payload.agencyId());
                AgencyProjection agency = agencyOpt.orElseGet(() ->
                        AgencyProjection.builder()
                                .agencyId(payload.agencyId())
                                .name("Bilinmeyen Dükkan")
                                .city("Bilinmiyor")
                                .phone("-")
                                .contactPerson("-")
                                .address("-")
                                .build()
                );

                PoolListing listing = poolListingRepository.findByVehicleId(payload.vehicleId())
                        .orElseGet(() -> PoolListing.builder()
                                .vehicleId(payload.vehicleId())
                                .build());

                listing.setAgencyId(payload.agencyId());
                listing.setAgencyName(agency.getName());
                listing.setAgencyCity(agency.getCity());
                listing.setAgencyPhone(agency.getPhone());
                listing.setContactPerson(agency.getContactPerson());
                listing.setAgencyAddress(agency.getAddress());

                listing.setPlate(payload.plate());
                listing.setBrand(payload.brand());
                listing.setModel(payload.model());
                listing.setModelYear(payload.modelYear());
                listing.setFuelType(payload.fuelType());
                listing.setTransmission(payload.transmission());
                listing.setDailyPoolPrice(payload.dailyPoolPrice());
                listing.setListedAt(Instant.now());

                poolListingRepository.save(listing);
                log.info("Vehicle added to/updated in pool: vehicleId={}, plate={}", payload.vehicleId(), payload.plate());
            } else {
                poolListingRepository.deleteByVehicleId(payload.vehicleId());
                log.info("Vehicle removed from pool: vehicleId={}, newStatus={}", payload.vehicleId(), payload.newStatus());
            }

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(envelope.eventId())
                    .topic("fleet.vehicle.status-changed")
                    .processedAt(Instant.now())
                    .build());

        } catch (Exception e) {
            log.error("Failed to process fleet.vehicle.status-changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing vehicle status change", e);
        }
    }
}

