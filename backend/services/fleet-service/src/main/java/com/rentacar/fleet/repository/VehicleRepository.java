package com.rentacar.fleet.repository;

import com.rentacar.fleet.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Page<Vehicle> findByAgencyId(UUID agencyId, Pageable pageable);
    boolean existsByAgencyIdAndPlate(UUID agencyId, String plate);
    Optional<Vehicle> findByIdAndAgencyId(UUID id, UUID agencyId);

    @Query("""
        SELECT v FROM Vehicle v JOIN FETCH v.inspection i
        WHERE (i.trafficInsuranceExpiry IS NOT NULL AND i.trafficInsuranceExpiry <= :thresholdDate)
           OR (i.cascoExpiry IS NOT NULL AND i.cascoExpiry <= :thresholdDate)
           OR (i.inspectionExpiry IS NOT NULL AND i.inspectionExpiry <= :thresholdDate)
    """)
    List<Vehicle> findVehiclesWithExpiringOrExpiredCompliance(@Param("thresholdDate") LocalDate thresholdDate);
}

