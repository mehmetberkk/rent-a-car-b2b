package com.rentacar.fleet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicle_inspections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    @Column(name = "traffic_insurance_expiry")
    private LocalDate trafficInsuranceExpiry;

    @Column(name = "casco_expiry")
    private LocalDate cascoExpiry;

    @Column(name = "inspection_expiry")
    private LocalDate inspectionExpiry;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

