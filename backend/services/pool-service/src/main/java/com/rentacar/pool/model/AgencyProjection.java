package com.rentacar.pool.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agency_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyProjection {

    @Id
    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String district;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Size(max = 200)
    @Column(name = "contact_person", nullable = false, length = 200)
    private String contactPerson;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

