package com.rentacar.identity;

import com.rentacar.identity.dto.AuthDtos.LoginRequest;
import com.rentacar.identity.dto.AuthDtos.RegisterRequest;
import com.rentacar.identity.model.ActivationToken;
import com.rentacar.identity.model.User;
import com.rentacar.identity.repository.ActivationTokenRepository;
import com.rentacar.identity.repository.UserRepository;
import com.rentacar.identity.service.AuthService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthIntegrationTest {

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
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @BeforeEach
    void cleanUp() {
        activationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Kullanıcı kaydı başarıyla oluşturulmalı, PENDING statüsünde kaydedilmeli ve aktivasyon jetonu üretilmelidir")
    void shouldRegisterUserAndCreatePendingAccount() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ahmet")
                .lastName("Yılmaz")
                .email("ahmet@example.com")
                .password("Password123")
                .build();

        authService.register(request);

        Optional<User> savedUser = userRepository.findByEmail("ahmet@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getStatus()).isEqualTo(User.UserStatus.PENDING);
        assertThat(savedUser.get().getFirstName()).isEqualTo("Ahmet");

        Optional<ActivationToken> token = activationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(savedUser.get().getId());
        assertThat(token).isPresent();
        assertThat(token.get().isUsed()).isFalse();
    }

    @Test
    @DisplayName("Aynı e-posta adresi ile mükerrer kayıt reddedilmelidir")
    void shouldRejectDuplicateEmailRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .email("mehmet@example.com")
                .password("Password123")
                .build();

        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zaten mevcut");
    }

    @Test
    @DisplayName("Geçerli aktivasyon jetonu ile hesap ACTIVE statüsüne geçmeli ve giriş yapılabilmelidir")
    void shouldActivateUserAndAllowLogin() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Can")
                .lastName("Kaya")
                .email("can@example.com")
                .password("Password123")
                .build();

        authService.register(request);
        User user = userRepository.findByEmail("can@example.com").orElseThrow();
        ActivationToken token = activationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();

        // Aktivasyon öncesi giriş denenir -> Hata beklenir
        LoginRequest loginRequest = LoginRequest.builder()
                .email("can@example.com")
                .password("Password123")
                .build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aktive edilmemiştir");

        // Aktivasyon yapılır
        authService.activate(token.getToken());

        User activatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(activatedUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);

        // Aktivasyon sonrası giriş başarılı olmalıdır
        var authResponse = authService.login(loginRequest);
        assertThat(authResponse.getAccessToken()).isNotBlank();
        assertThat(authResponse.getUser().getEmail()).isEqualTo("can@example.com");
    }
}

