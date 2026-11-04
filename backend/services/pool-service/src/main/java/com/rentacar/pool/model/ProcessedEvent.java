package com.rentacar.pool.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String topic;

    @NotNull
    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
}

