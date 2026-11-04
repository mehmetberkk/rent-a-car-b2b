package com.rentacar.pool.repository;

import com.rentacar.pool.model.PoolListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PoolListingRepository extends JpaRepository<PoolListing, UUID>, JpaSpecificationExecutor<PoolListing> {

    Optional<PoolListing> findByVehicleId(UUID vehicleId);

    @Modifying
    @Query("DELETE FROM PoolListing p WHERE p.vehicleId = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") UUID vehicleId);

    @Modifying
    @Query("DELETE FROM PoolListing p WHERE p.agencyId = :agencyId")
    void deleteByAgencyId(@Param("agencyId") UUID agencyId);

    @Query("SELECT DISTINCT p.agencyCity FROM PoolListing p ORDER BY p.agencyCity")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT p.brand FROM PoolListing p ORDER BY p.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT p.fuelType FROM PoolListing p ORDER BY p.fuelType")
    List<String> findDistinctFuelTypes();

    @Query("SELECT DISTINCT p.transmission FROM PoolListing p ORDER BY p.transmission")
    List<String> findDistinctTransmissions();
}

