package com.rentacar.pool.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pool_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoolListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "vehicle_id", nullable = false, unique = true)
    private UUID vehicleId;

    @NotNull
    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @NotBlank
    @Size(max = 200)
    @Column(name = "agency_name", nullable = false, length = 200)
    private String agencyName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "agency_city", nullable = false, length = 100)
    private String agencyCity;

    @NotBlank
    @Size(max = 20)
    @Column(name = "agency_phone", nullable = false, length = 20)
    private String agencyPhone;

    @NotBlank
    @Size(max = 200)
    @Column(name = "contact_person", nullable = false, length = 200)
    private String contactPerson;

    @NotBlank
    @Column(name = "agency_address", nullable = false, columnDefinition = "TEXT")
    private String agencyAddress;

    @NotBlank
    @Size(max = 15)
    @Column(nullable = false, length = 15)
    private String plate;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String brand;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String model;

    @NotNull
    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @NotBlank
    @Size(max = 20)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private String fuelType;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String transmission;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "daily_pool_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPoolPrice;

    @NotNull
    @Column(name = "listed_at", nullable = false)
    @Builder.Default
    private Instant listedAt = Instant.now();
}

