package com.rentacar.identity.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agencies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @NotBlank
    @Size(min = 2, max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Size(min = 2, max = 100)
    @Column(name = "district", length = 100)
    private String district;

    @NotBlank
    @Size(min = 10, max = 20)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Size(min = 2, max = 200)
    @Column(name = "contact_person", nullable = false, length = 200)
    private String contactPerson;

    @NotBlank
    @Size(min = 10, max = 500)
    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

