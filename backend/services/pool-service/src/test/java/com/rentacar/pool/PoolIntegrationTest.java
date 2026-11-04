package com.rentacar.pool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.events.EventEnvelope;
import com.rentacar.common.events.fleet.VehicleStatusChangedPayload;
import com.rentacar.common.events.identity.AgencyUpsertedPayload;
import com.rentacar.pool.consumer.AgencyUpsertedConsumer;
import com.rentacar.pool.consumer.VehicleStatusConsumer;
import com.rentacar.pool.dto.PoolDtos.PoolListingDto;
import com.rentacar.pool.model.AgencyProjection;
import com.rentacar.pool.model.PoolListing;
import com.rentacar.pool.repository.AgencyProjectionRepository;
import com.rentacar.pool.repository.PoolListingRepository;
import com.rentacar.pool.repository.ProcessedEventRepository;
import com.rentacar.pool.service.PoolListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PoolIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pool_db")
            .withUsername("rentacar")
            .withPassword("rentacar_secret");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private AgencyProjectionRepository agencyRepository;

    @Autowired
    private PoolListingRepository poolListingRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private AgencyUpsertedConsumer agencyUpsertedConsumer;

    @Autowired
    private VehicleStatusConsumer vehicleStatusConsumer;

    @Autowired
    private PoolListingService poolListingService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        poolListingRepository.deleteAll();
        agencyRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Dükkan ve araç eventleri tüketildiğinde havuza denormalize kayıt eklenmeli, durum değiştiğinde silinmelidir")
    void shouldHandleStatusChangeAndDenormalizeAgencyDetails() throws Exception {
        UUID agencyId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        // 1. Send Agency Upserted Event
        AgencyUpsertedPayload agencyPayload = new AgencyUpsertedPayload(
                agencyId,
                "Ege Rent A Car",
                "İzmir",
                "Bornova",
                "02321112233",
                "Mehmet Demir",
                "Bornova Meydan No: 5"
        );
        EventEnvelope<AgencyUpsertedPayload> agencyEnvelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "identity.agency.upserted",
                Instant.now(),
                UUID.randomUUID(),
                agencyPayload
        );
        agencyUpsertedConsumer.consumeAgencyUpserted(objectMapper.writeValueAsString(agencyEnvelope));

        AgencyProjection savedAgency = agencyRepository.findById(agencyId).orElse(null);
        assertThat(savedAgency).isNotNull();
        assertThat(savedAgency.getName()).isEqualTo("Ege Rent A Car");

        // 2. Send Vehicle Status Changed Event to IN_POOL
        VehicleStatusChangedPayload vehiclePayload = new VehicleStatusChangedPayload(
                vehicleId,
                agencyId,
                "35EGE35",
                "Toyota",
                "Corolla",
                2023,
                "HYBRID",
                "AUTOMATIC",
                BigDecimal.valueOf(1800.00),
                "IN_POOL",
                "AVAILABLE"
        );
        EventEnvelope<VehicleStatusChangedPayload> vehicleEnvelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "fleet.vehicle.status-changed",
                Instant.now(),
                UUID.randomUUID(),
                vehiclePayload
        );
        vehicleStatusConsumer.consumeVehicleStatusChanged(objectMapper.writeValueAsString(vehicleEnvelope));

        // 3. Verify Pool Listing exists with denormalized agency contact info
        PoolListing listing = poolListingRepository.findByVehicleId(vehicleId).orElse(null);
        assertThat(listing).isNotNull();
        assertThat(listing.getPlate()).isEqualTo("35EGE35");
        assertThat(listing.getAgencyName()).isEqualTo("Ege Rent A Car");
        assertThat(listing.getAgencyCity()).isEqualTo("İzmir");
        assertThat(listing.getContactPerson()).isEqualTo("Mehmet Demir");

        // 4. Send Vehicle Status Changed Event to AVAILABLE
        VehicleStatusChangedPayload availablePayload = new VehicleStatusChangedPayload(
                vehicleId,
                agencyId,
                "35EGE35",
                "Toyota",
                "Corolla",
                2023,
                "HYBRID",
                "AUTOMATIC",
                BigDecimal.valueOf(1800.00),
                "AVAILABLE",
                "IN_POOL"
        );
        EventEnvelope<VehicleStatusChangedPayload> availableEnvelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "fleet.vehicle.status-changed",
                Instant.now(),
                UUID.randomUUID(),
                availablePayload
        );
        vehicleStatusConsumer.consumeVehicleStatusChanged(objectMapper.writeValueAsString(availableEnvelope));

        // 5. Verify vehicle was removed from pool
        assertThat(poolListingRepository.findByVehicleId(vehicleId)).isEmpty();
    }

    @Test
    @DisplayName("Ortak havuz sorgularında filtreleme ve dükkan sahibinin kendi araçlarını hariç tutma kuralları çalışmalıdır")
    void shouldFilterAndExcludeOwnAgencies() {
        UUID myAgencyId = UUID.randomUUID();
        UUID otherAgencyId = UUID.randomUUID();

        // Listing for other agency in Istanbul
        poolListingRepository.save(PoolListing.builder()
                .vehicleId(UUID.randomUUID())
                .agencyId(otherAgencyId)
                .agencyName("Boğaziçi Filo")
                .agencyCity("İstanbul")
                .agencyPhone("02123334455")
                .contactPerson("Ali Veli")
                .agencyAddress("Kadıköy")
                .plate("34BOG34")
                .brand("BMW")
                .model("320i")
                .modelYear(2023)
                .fuelType("GASOLINE")
                .transmission("AUTOMATIC")
                .dailyPoolPrice(BigDecimal.valueOf(3500.00))
                .build());

        // Listing for my agency in Istanbul
        poolListingRepository.save(PoolListing.builder()
                .vehicleId(UUID.randomUUID())
                .agencyId(myAgencyId)
                .agencyName("Benim Filom")
                .agencyCity("İstanbul")
                .agencyPhone("02129998877")
                .contactPerson("Kullanıcı")
                .agencyAddress("Şişli")
                .plate("34MY34")
                .brand("Mercedes")
                .model("C200")
                .modelYear(2024)
                .fuelType("HYBRID")
                .transmission("AUTOMATIC")
                .dailyPoolPrice(BigDecimal.valueOf(4000.00))
                .build());

        // 1. Search with excludeOwn = true
        Page<PoolListingDto> result = poolListingService.searchListings(
                "İstanbul", null, null, null, null, null,
                true, List.of(myAgencyId), PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getPlate()).isEqualTo("34BOG34");
        assertThat(result.getContent().get(0).getAgency().getName()).isEqualTo("Boğaziçi Filo");

        // 2. Search without excludeOwn
        Page<PoolListingDto> allResult = poolListingService.searchListings(
                "İstanbul", null, null, null, null, null,
                false, List.of(myAgencyId), PageRequest.of(0, 10)
        );
        assertThat(allResult.getTotalElements()).isEqualTo(2);
    }
}

