package com.rentacar.identity.repository;

import com.rentacar.identity.model.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {
    Optional<ActivationToken> findByToken(String token);
    Optional<ActivationToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}

