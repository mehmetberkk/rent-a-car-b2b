package com.rentacar.identity;

import com.rentacar.identity.dto.AgencyDtos.CreateAgencyRequest;
import com.rentacar.identity.dto.AgencyDtos.CreateAgencyResponse;
import com.rentacar.identity.model.Agency;
import com.rentacar.identity.model.User;
import com.rentacar.identity.repository.AgencyRepository;
import com.rentacar.identity.repository.UserRepository;
import com.rentacar.identity.service.AgencyService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AgencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("identity_db")
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
    private AgencyService agencyService;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        agencyRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .firstName("Ali")
                .lastName("Kaya")
                .email("ali@example.com")
                .passwordHash("hashed")
                .status(User.UserStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Dükkan oluşturulmalı, veritabanına kaydedilmeli ve güncel JWT döndürülmelidir")
    void shouldCreateAgencyAndReturnRefreshedToken() {
        CreateAgencyRequest request = CreateAgencyRequest.builder()
                .name("Yıldız Oto Kiralama")
                .city("İstanbul")
                .district("Kadıköy")
                .phone("05321112233")
                .contactPerson("Ali Kaya")
                .address("Bağdat Cad. No:123")
                .build();

        CreateAgencyResponse response = agencyService.createAgency(testUser.getId(), testUser.getEmail(), request);

        assertThat(response.getAgency()).isNotNull();
        assertThat(response.getAgency().getName()).isEqualTo("Yıldız Oto Kiralama");
        assertThat(response.getRefreshedToken().getAgencyIds()).contains(response.getAgency().getId());
        assertThat(response.getRefreshedToken().getAccessToken()).isNotBlank();

        List<Agency> agencies = agencyRepository.findByOwnerId(testUser.getId());
        assertThat(agencies).hasSize(1);
    }

    @Test
    @DisplayName("Başka bir kullanıcının dükkanına erişim engellenmelidir")
    void shouldPreventAccessToOtherUsersAgency() {
        UUID otherUserId = UUID.randomUUID();

        CreateAgencyRequest request = CreateAgencyRequest.builder()
                .name("Başkasına Ait Dükkan")
                .city("Ankara")
                .phone("05322223344")
                .contactPerson("Veli")
                .address("Kızılay")
                .build();

        CreateAgencyResponse response = agencyService.createAgency(otherUserId, "other@example.com", request);

        assertThatThrownBy(() -> agencyService.getAgencyById(response.getAgency().getId(), testUser.getId()))
                .isInstanceOf(SecurityException.class);
    }
}

