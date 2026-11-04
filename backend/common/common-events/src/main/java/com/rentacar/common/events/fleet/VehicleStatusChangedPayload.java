package com.rentacar.common.events.fleet;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleStatusChangedPayload(
        UUID vehicleId,
        UUID agencyId,
        String plate,
        String brand,
        String model,
        int modelYear,
        String fuelType,
        String transmission,
        BigDecimal dailyPoolPrice,
        String newStatus,
        String previousStatus
) {
}

