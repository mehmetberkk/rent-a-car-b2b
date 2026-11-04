package com.rentacar.pool.repository;

import com.rentacar.pool.model.AgencyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgencyProjectionRepository extends JpaRepository<AgencyProjection, UUID> {
}

