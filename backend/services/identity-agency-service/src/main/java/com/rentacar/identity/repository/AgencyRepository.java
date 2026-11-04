package com.rentacar.identity.repository;

import com.rentacar.identity.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {
    List<Agency> findByOwnerId(UUID ownerId);
    Optional<Agency> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("SELECT a.id FROM Agency a WHERE a.ownerId = :ownerId")
    List<UUID> findAgencyIdsByOwnerId(@Param("ownerId") UUID ownerId);
}

