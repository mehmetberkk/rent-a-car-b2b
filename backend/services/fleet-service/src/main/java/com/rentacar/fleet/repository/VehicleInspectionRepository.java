package com.rentacar.fleet.repository;

import com.rentacar.fleet.model.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, UUID> {
    Optional<VehicleInspection> findByVehicleId(UUID vehicleId);
}

