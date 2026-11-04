package com.rentacar.pool.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.identity.AgencyUpsertedPayload;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgencyUpsertedConsumer {

    private final AgencyProjectionRepository agencyProjectionRepository;
    private final PoolListingRepository poolListingRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "identity.agency.upserted", groupId = "pool-service-agency-group")
    public void consumeAgencyUpserted(String message) {
        try {
            EventEnvelope<AgencyUpsertedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<>() {}
            );

            if (processedEventRepository.existsById(envelope.eventId())) {
                log.info("Duplicate identity.agency.upserted event ignored: {}", envelope.eventId());
                return;
            }

            AgencyUpsertedPayload payload = envelope.payload();
            log.info("Processing agency upserted event: agencyId={}, name={}", payload.agencyId(), payload.name());

            AgencyProjection projection = agencyProjectionRepository.findById(payload.agencyId())
                    .orElseGet(() -> AgencyProjection.builder().agencyId(payload.agencyId()).build());

            projection.setName(payload.name());
            projection.setCity(payload.city());
            projection.setDistrict(payload.district());
            projection.setPhone(payload.phone());
            projection.setContactPerson(payload.contactPerson());
            projection.setAddress(payload.address());
            projection.setUpdatedAt(Instant.now());
            agencyProjectionRepository.save(projection);

            // Update any existing pool listings for this agency
            List<PoolListing> listings = poolListingRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("agencyId"), payload.agencyId())
            );

            for (PoolListing listing : listings) {
                listing.setAgencyName(payload.name());
                listing.setAgencyCity(payload.city());
                listing.setAgencyPhone(payload.phone());
                listing.setContactPerson(payload.contactPerson());
                listing.setAgencyAddress(payload.address());
                poolListingRepository.save(listing);
            }

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(envelope.eventId())
                    .topic("identity.agency.upserted")
                    .processedAt(Instant.now())
                    .build());

            log.info("Agency projection updated successfully for agencyId={}", payload.agencyId());
        } catch (Exception e) {
            log.error("Failed to process identity.agency.upserted event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing agency upserted event", e);
        }
    }
}

