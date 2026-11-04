package com.rentacar.common.events.fleet;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VehicleComplianceAlertPayload(
        UUID vehicleId,
        UUID agencyId,
        String plate,
        String brand,
        String model,
        List<ComplianceAlertItem> alerts
) {
    public record ComplianceAlertItem(
            String type, // TRAFFIC_INSURANCE, CASCO, INSPECTION
            LocalDate expiryDate,
            long daysRemaining,
            String severity // EXPIRED, EXPIRING_SOON
    ) {}
}

