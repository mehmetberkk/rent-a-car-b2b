package com.rentacar.fleet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vehicles",
        uniqueConstraints = @UniqueConstraint(name = "uq_agency_plate", columnNames = {"agency_id", "plate"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @NotBlank
    @Size(max = 15)
    @Column(name = "plate", nullable = false, length = 15)
    private String plate;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @NotNull
    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission", nullable = false, length = 20)
    private Transmission transmission;

    @NotNull
    @DecimalMin(value = "0.01", message = "Günlük kira bedeli sıfırdan büyük olmalıdır")
    @Column(name = "daily_pool_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPoolPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @OneToOne(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VehicleInspection inspection;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum FuelType {
        GASOLINE,
        DIESEL,
        ELECTRIC,
        HYBRID
    }

    public enum Transmission {
        MANUAL,
        AUTOMATIC
    }

    public enum VehicleStatus {
        AVAILABLE,
        RENTED,
        IN_POOL
    }
}

