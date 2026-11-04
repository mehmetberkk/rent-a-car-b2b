package com.rentacar.fleet;

import com.rentacar.fleet.dto.VehicleDtos.CreateVehicleRequest;
import com.rentacar.fleet.dto.VehicleDtos.StatusChangedResponse;
import com.rentacar.fleet.dto.VehicleDtos.VehicleDto;
import com.rentacar.fleet.model.Vehicle.FuelType;
import com.rentacar.fleet.model.Vehicle.Transmission;
import com.rentacar.fleet.model.Vehicle.VehicleStatus;
import com.rentacar.fleet.repository.VehicleInspectionRepository;
import com.rentacar.fleet.repository.VehicleRepository;
import com.rentacar.fleet.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class VehicleStatusIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fleet_db")
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
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleInspectionRepository inspectionRepository;

    private UUID agencyId;

    @BeforeEach
    void setUp() {
        inspectionRepository.deleteAll();
        vehicleRepository.deleteAll();
        agencyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Araç durumu AVAILABLE -> IN_POOL -> RENTED -> AVAILABLE geçişlerini başarıyla tamamlamalıdır")
    void shouldTransitionVehicleStatusSuccessfully() {
        CreateVehicleRequest createReq = CreateVehicleRequest.builder()
                .agencyId(agencyId)
                .plate("34POOL01")
                .brand("Honda")
                .model("Civic")
                .modelYear(2023)
                .fuelType(FuelType.GASOLINE)
                .transmission(Transmission.AUTOMATIC)
                .dailyPoolPrice(BigDecimal.valueOf(1750.00))
                .build();

        VehicleDto vehicle = vehicleService.createVehicle(createReq, List.of(agencyId));
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        // Transition 1: AVAILABLE -> IN_POOL
        StatusChangedResponse step1 = vehicleService.updateStatus(vehicle.getId(), VehicleStatus.IN_POOL, List.of(agencyId));
        assertThat(step1.getStatus()).isEqualTo(VehicleStatus.IN_POOL);
        assertThat(step1.getPreviousStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        // Transition 2: IN_POOL -> RENTED
        StatusChangedResponse step2 = vehicleService.updateStatus(vehicle.getId(), VehicleStatus.RENTED, List.of(agencyId));
        assertThat(step2.getStatus()).isEqualTo(VehicleStatus.RENTED);
        assertThat(step2.getPreviousStatus()).isEqualTo(VehicleStatus.IN_POOL);

        // Transition 3: RENTED -> AVAILABLE
        StatusChangedResponse step3 = vehicleService.updateStatus(vehicle.getId(), VehicleStatus.AVAILABLE, List.of(agencyId));
        assertThat(step3.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(step3.getPreviousStatus()).isEqualTo(VehicleStatus.RENTED);
    }
}

