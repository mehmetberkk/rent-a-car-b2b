package com.rentacar.fleet;

import com.rentacar.fleet.dto.VehicleDtos.CreateVehicleRequest;
import com.rentacar.fleet.dto.VehicleDtos.UpdateStatusRequest;
import com.rentacar.fleet.dto.VehicleDtos.UpdateVehicleRequest;
import com.rentacar.fleet.dto.VehicleDtos.VehicleDto;
import com.rentacar.fleet.model.Vehicle.FuelType;
import com.rentacar.fleet.model.Vehicle.Transmission;
import com.rentacar.fleet.model.Vehicle.VehicleStatus;
import com.rentacar.fleet.repository.VehicleInspectionRepository;
import com.rentacar.fleet.repository.VehicleRepository;
import com.rentacar.fleet.scheduler.VehicleComplianceScheduler;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class VehicleIntegrationTest {

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

    @Autowired
    private VehicleComplianceScheduler complianceScheduler;

    private UUID agencyA;
    private UUID agencyB;

    @BeforeEach
    void cleanUp() {
        inspectionRepository.deleteAll();
        vehicleRepository.deleteAll();
        agencyA = UUID.randomUUID();
        agencyB = UUID.randomUUID();
    }

    @Test
    @DisplayName("Dükkana ait yeni araç başarıyla eklenmeli ve bilgileri doğrulanmalıdır")
    void shouldCreateVehicleSuccessfully() {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .agencyId(agencyA)
                .plate("34ABC123")
                .brand("Renault")
                .model("Clio")
                .modelYear(2022)
                .fuelType(FuelType.GASOLINE)
                .transmission(Transmission.AUTOMATIC)
                .dailyPoolPrice(BigDecimal.valueOf(1500.00))
                .trafficInsuranceExpiry(LocalDate.now().plusMonths(6))
                .cascoExpiry(LocalDate.now().plusMonths(8))
                .inspectionExpiry(LocalDate.now().plusMonths(12))
                .build();

        VehicleDto vehicle = vehicleService.createVehicle(request, List.of(agencyA));

        assertThat(vehicle.getId()).isNotNull();
        assertThat(vehicle.getPlate()).isEqualTo("34ABC123");
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getTrafficInsuranceExpiry()).isNotNull();
    }

    @Test
    @DisplayName("Aynı dükkanda mükerrer plaka kaydı reddedilmelidir, ancak farklı dükkanda eklenebilmelidir")
    void shouldEnforcePlateUniquenessPerAgency() {
        CreateVehicleRequest req1 = CreateVehicleRequest.builder()
                .agencyId(agencyA)
                .plate("34XYZ99")
                .brand("Fiat")
                .model("Egea")
                .modelYear(2021)
                .fuelType(FuelType.DIESEL)
                .transmission(Transmission.MANUAL)
                .dailyPoolPrice(BigDecimal.valueOf(1200.00))
                .build();

        vehicleService.createVehicle(req1, List.of(agencyA));

        // Same plate, same agency -> should fail
        assertThatThrownBy(() -> vehicleService.createVehicle(req1, List.of(agencyA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aynı plakaya sahip başka bir araç zaten mevcut");

        // Same plate, different agency -> should succeed
        CreateVehicleRequest req2 = CreateVehicleRequest.builder()
                .agencyId(agencyB)
                .plate("34XYZ99")
                .brand("Fiat")
                .model("Egea")
                .modelYear(2021)
                .fuelType(FuelType.DIESEL)
                .transmission(Transmission.MANUAL)
                .dailyPoolPrice(BigDecimal.valueOf(1200.00))
                .build();

        VehicleDto vehicle2 = vehicleService.createVehicle(req2, List.of(agencyB));
        assertThat(vehicle2.getId()).isNotNull();
    }

    @Test
    @DisplayName("Kullanıcı yetkisi olmayan dükkana araç ekleyememelidir")
    void shouldDenyAccessToUnauthorizedAgency() {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .agencyId(agencyA)
                .plate("06ANK06")
                .brand("Toyota")
                .model("Corolla")
                .modelYear(2023)
                .fuelType(FuelType.HYBRID)
                .transmission(Transmission.AUTOMATIC)
                .dailyPoolPrice(BigDecimal.valueOf(2000.00))
                .build();

        assertThatThrownBy(() -> vehicleService.createVehicle(request, List.of(agencyB)))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("yetkiniz bulunmamaktadır");
    }

    @Test
    @DisplayName("Araç durumu ve muayene bilgileri güncellenebilmelidir")
    void shouldUpdateVehicleAndStatus() {
        VehicleDto created = vehicleService.createVehicle(
                CreateVehicleRequest.builder()
                        .agencyId(agencyA)
                        .plate("35IZM35")
                        .brand("Ford")
                        .model("Focus")
                        .modelYear(2020)
                        .fuelType(FuelType.DIESEL)
                        .transmission(Transmission.MANUAL)
                        .dailyPoolPrice(BigDecimal.valueOf(1400.00))
                        .trafficInsuranceExpiry(LocalDate.now().plusDays(10))
                        .build(),
                List.of(agencyA)
        );

        // Status update to IN_POOL
        var statusResponse = vehicleService.updateStatus(created.getId(), VehicleStatus.IN_POOL, List.of(agencyA));
        assertThat(statusResponse.getStatus()).isEqualTo(VehicleStatus.IN_POOL);
        assertThat(statusResponse.getPreviousStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        // Update details
        UpdateVehicleRequest updateReq = UpdateVehicleRequest.builder()
                .plate("35IZM35")
                .brand("Ford")
                .model("Focus Titanium")
                .modelYear(2020)
                .fuelType(FuelType.DIESEL)
                .transmission(Transmission.MANUAL)
                .dailyPoolPrice(BigDecimal.valueOf(1600.00))
                .trafficInsuranceExpiry(LocalDate.now().plusMonths(12))
                .build();

        VehicleDto updated = vehicleService.updateVehicle(created.getId(), updateReq, List.of(agencyA));
        assertThat(updated.getModel()).isEqualTo("Focus Titanium");
        assertThat(updated.getDailyPoolPrice()).isEqualByComparingTo(BigDecimal.valueOf(1600.00));
    }

    @Test
    @DisplayName("Süresi 30 günün altına inen araçlar için cron görev başarıyla çalışmalıdır")
    void shouldTriggerComplianceAlertsForExpiringVehicles() {
        // Vehicle with insurance expiring in 5 days
        vehicleService.createVehicle(
                CreateVehicleRequest.builder()
                        .agencyId(agencyA)
                        .plate("16BUR16")
                        .brand("Volkswagen")
                        .model("Passat")
                        .modelYear(2022)
                        .fuelType(FuelType.DIESEL)
                        .transmission(Transmission.AUTOMATIC)
                        .dailyPoolPrice(BigDecimal.valueOf(2500.00))
                        .trafficInsuranceExpiry(LocalDate.now().plusDays(5))
                        .cascoExpiry(LocalDate.now().plusMonths(6))
                        .inspectionExpiry(LocalDate.now().plusMonths(6))
                        .build(),
                List.of(agencyA)
        );

        // Run scheduler
        complianceScheduler.checkComplianceExpirations();
        // Passed without exception
    }
}

