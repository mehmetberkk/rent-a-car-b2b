package com.rentacar.fleet.scheduler;

import com.rentacar.common.events.fleet.VehicleComplianceAlertPayload;
import com.rentacar.common.events.fleet.VehicleComplianceAlertPayload.ComplianceAlertItem;
import com.rentacar.fleet.event.VehicleEventProducer;
import com.rentacar.fleet.model.Vehicle;
import com.rentacar.fleet.model.VehicleInspection;
import com.rentacar.fleet.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleComplianceScheduler {

    private final VehicleRepository vehicleRepository;
    private final VehicleEventProducer eventProducer;

    // Her gün gece 02:00'da çalışır (test ortamında manuel de çağrılabilir)
    @Scheduled(cron = "${app.compliance.cron:0 0 2 * * *}")
    public void checkComplianceExpirations() {
        log.info("Running compliance expiration check scheduled task...");
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        List<Vehicle> vehicles = vehicleRepository.findVehiclesWithExpiringOrExpiredCompliance(threshold);
        log.info("Found {} vehicles requiring compliance alerts", vehicles.size());

        for (Vehicle vehicle : vehicles) {
            VehicleInspection inspection = vehicle.getInspection();
            if (inspection == null) continue;

            List<ComplianceAlertItem> alerts = new ArrayList<>();

            checkDate(inspection.getTrafficInsuranceExpiry(), "TRAFFIC_INSURANCE", today, alerts);
            checkDate(inspection.getCascoExpiry(), "CASCO", today, alerts);
            checkDate(inspection.getInspectionExpiry(), "INSPECTION", today, alerts);

            if (!alerts.isEmpty()) {
                VehicleComplianceAlertPayload payload = new VehicleComplianceAlertPayload(
                        vehicle.getId(),
                        vehicle.getAgencyId(),
                        vehicle.getPlate(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        alerts
                );
                eventProducer.publishComplianceAlert(payload, UUID.randomUUID());
            }
        }
    }

    private void checkDate(LocalDate date, String type, LocalDate today, List<ComplianceAlertItem> alerts) {
        if (date == null) return;

        long daysRemaining = ChronoUnit.DAYS.between(today, date);
        if (daysRemaining <= 30) {
            String severity = daysRemaining < 0 ? "EXPIRED" : "EXPIRING_SOON";
            alerts.add(new ComplianceAlertItem(type, date, daysRemaining, severity));
        }
    }
}

