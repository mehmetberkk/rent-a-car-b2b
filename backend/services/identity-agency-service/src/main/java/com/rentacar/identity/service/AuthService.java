package com.rentacar.identity.service;

import com.rentacar.common.events.identity.UserRegisteredPayload;
import com.rentacar.identity.dto.AuthDtos.*;
import com.rentacar.identity.event.IdentityEventProducer;
import com.rentacar.identity.model.ActivationToken;
import com.rentacar.identity.model.User;
import com.rentacar.identity.repository.ActivationTokenRepository;
import com.rentacar.identity.repository.AgencyRepository;
import com.rentacar.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final AgencyRepository agencyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IdentityEventProducer eventProducer;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu e-posta adresi ile kayıtlı bir kullanıcı zaten mevcut: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(User.UserStatus.PENDING)
                .build();

        user = userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        ActivationToken token = ActivationToken.builder()
                .userId(user.getId())
                .token(tokenValue)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .used(false)
                .build();
        activationTokenRepository.save(token);

        UserRegisteredPayload payload = new UserRegisteredPayload(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                tokenValue
        );
        eventProducer.publishUserRegistered(payload, UUID.randomUUID());

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void activate(String tokenValue) {
        ActivationToken token = activationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz aktivasyon jetonu"));

        if (token.isUsed()) {
            throw new IllegalStateException("Bu aktivasyon bağlantısı daha önce kullanılmış");
        }

        if (token.isExpired()) {
            throw new IllegalStateException("Aktivasyon bağlantısının süresi dolmuş. Lütfen yeni bir aktivasyon e-postası talep edin.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsed(true);
        activationTokenRepository.save(token);
        log.info("User successfully activated: {}", user.getEmail());
    }

    @Transactional
    public void resendActivation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı"));

        if (user.getStatus() == User.UserStatus.ACTIVE) {
            throw new IllegalStateException("Hesabınız zaten aktif durumdadır. Giriş yapabilirsiniz.");
        }

        String tokenValue = UUID.randomUUID().toString();
        ActivationToken token = ActivationToken.builder()
                .userId(user.getId())
                .token(tokenValue)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .used(false)
                .build();
        activationTokenRepository.save(token);

        UserRegisteredPayload payload = new UserRegisteredPayload(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                tokenValue
        );
        eventProducer.publishUserRegistered(payload, UUID.randomUUID());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new SecurityException("E-posta adresi veya şifre hatalı"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new SecurityException("E-posta adresi veya şifre hatalı");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalStateException("Hesabınız henüz aktive edilmemiştir. Lütfen e-postanıza gönderilen aktivasyon bağlantısını onaylayınız.");
        }

        List<UUID> agencyIds = agencyRepository.findAgencyIdsByOwnerId(user.getId());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), agencyIds);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .user(UserDto.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build())
                .agencyIds(agencyIds)
                .build();
    }

    @Transactional(readOnly = true)
    public RefreshedTokenResponse refresh(String tokenHeader) {
        String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
        Claims claims = jwtService.parseToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);

        List<UUID> agencyIds = agencyRepository.findAgencyIdsByOwnerId(userId);
        String newToken = jwtService.generateToken(userId, email, agencyIds);

        return RefreshedTokenResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .agencyIds(agencyIds)
                .build();
    }
}

