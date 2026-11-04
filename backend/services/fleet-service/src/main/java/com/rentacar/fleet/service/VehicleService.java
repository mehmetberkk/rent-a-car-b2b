package com.rentacar.fleet.service;

import com.rentacar.common.events.fleet.VehicleStatusChangedPayload;
import com.rentacar.fleet.dto.VehicleDtos.*;
import com.rentacar.fleet.event.VehicleEventProducer;
import com.rentacar.fleet.model.Vehicle;
import com.rentacar.fleet.model.Vehicle.VehicleStatus;
import com.rentacar.fleet.model.VehicleInspection;
import com.rentacar.fleet.repository.VehicleInspectionRepository;
import com.rentacar.fleet.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleInspectionRepository inspectionRepository;
    private final VehicleEventProducer eventProducer;

    public void verifyAgencyOwnership(UUID agencyId, List<UUID> allowedAgencyIds) {
        if (allowedAgencyIds == null || !allowedAgencyIds.contains(agencyId)) {
            throw new SecurityException("Bu dükkan üzerinde işlem yapma yetkiniz bulunmamaktadır: " + agencyId);
        }
    }

    @Transactional
    public VehicleDto createVehicle(CreateVehicleRequest request, List<UUID> allowedAgencyIds) {
        verifyAgencyOwnership(request.getAgencyId(), allowedAgencyIds);

        if (vehicleRepository.existsByAgencyIdAndPlate(request.getAgencyId(), request.getPlate())) {
            throw new IllegalArgumentException("Bu dükkanda aynı plakaya sahip başka bir araç zaten mevcut: " + request.getPlate());
        }

        Vehicle vehicle = Vehicle.builder()
                .agencyId(request.getAgencyId())
                .plate(request.getPlate())
                .brand(request.getBrand())
                .model(request.getModel())
                .modelYear(request.getModelYear())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .dailyPoolPrice(request.getDailyPoolPrice())
                .status(VehicleStatus.AVAILABLE)
                .build();

        vehicle = vehicleRepository.save(vehicle);

        VehicleInspection inspection = VehicleInspection.builder()
                .vehicle(vehicle)
                .trafficInsuranceExpiry(request.getTrafficInsuranceExpiry())
                .cascoExpiry(request.getCascoExpiry())
                .inspectionExpiry(request.getInspectionExpiry())
                .build();

        inspection = inspectionRepository.save(inspection);
        vehicle.setInspection(inspection);

        log.info("Vehicle created: id={}, plate={}, agencyId={}", vehicle.getId(), vehicle.getPlate(), vehicle.getAgencyId());
        return toDto(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<VehicleDto> getVehiclesByAgency(UUID agencyId, List<UUID> allowedAgencyIds, Pageable pageable) {
        verifyAgencyOwnership(agencyId, allowedAgencyIds);
        return vehicleRepository.findByAgencyId(agencyId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public VehicleDto getVehicleById(UUID vehicleId, List<UUID> allowedAgencyIds) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Araç bulunamadı: " + vehicleId));
        verifyAgencyOwnership(vehicle.getAgencyId(), allowedAgencyIds);
        return toDto(vehicle);
    }

    @Transactional
    public VehicleDto updateVehicle(UUID vehicleId, UpdateVehicleRequest request, List<UUID> allowedAgencyIds) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Araç bulunamadı: " + vehicleId));
        verifyAgencyOwnership(vehicle.getAgencyId(), allowedAgencyIds);

        if (!vehicle.getPlate().equalsIgnoreCase(request.getPlate()) &&
                vehicleRepository.existsByAgencyIdAndPlate(vehicle.getAgencyId(), request.getPlate())) {
            throw new IllegalArgumentException("Bu dükkanda aynı plakaya sahip başka bir araç zaten mevcut: " + request.getPlate());
        }

        vehicle.setPlate(request.getPlate());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setModelYear(request.getModelYear());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setDailyPoolPrice(request.getDailyPoolPrice());

        VehicleInspection inspection = inspectionRepository.findByVehicleId(vehicleId)
                .orElseGet(() -> VehicleInspection.builder().vehicle(vehicle).build());

        inspection.setTrafficInsuranceExpiry(request.getTrafficInsuranceExpiry());
        inspection.setCascoExpiry(request.getCascoExpiry());
        inspection.setInspectionExpiry(request.getInspectionExpiry());
        inspectionRepository.save(inspection);
        vehicle.setInspection(inspection);

        vehicle = vehicleRepository.save(vehicle);

        if (vehicle.getStatus() == VehicleStatus.IN_POOL) {
            publishStatusChangedEvent(vehicle, VehicleStatus.IN_POOL, VehicleStatus.IN_POOL.name());
        }

        return toDto(vehicle);
    }

    @Transactional
    public StatusChangedResponse updateStatus(UUID vehicleId, VehicleStatus newStatus, List<UUID> allowedAgencyIds) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Araç bulunamadı: " + vehicleId));
        verifyAgencyOwnership(vehicle.getAgencyId(), allowedAgencyIds);

        VehicleStatus previousStatus = vehicle.getStatus();
        vehicle.setStatus(newStatus);
        vehicle = vehicleRepository.save(vehicle);

        publishStatusChangedEvent(vehicle, newStatus, previousStatus.name());

        return StatusChangedResponse.builder()
                .id(vehicle.getId())
                .plate(vehicle.getPlate())
                .status(newStatus)
                .previousStatus(previousStatus)
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deleteVehicle(UUID vehicleId, List<UUID> allowedAgencyIds) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Araç bulunamadı: " + vehicleId));
        verifyAgencyOwnership(vehicle.getAgencyId(), allowedAgencyIds);

        if (vehicle.getStatus() == VehicleStatus.IN_POOL) {
            publishStatusChangedEvent(vehicle, VehicleStatus.AVAILABLE, "DELETED");
        }

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: id={}, plate={}", vehicleId, vehicle.getPlate());
    }

    private void publishStatusChangedEvent(Vehicle vehicle, VehicleStatus newStatus, String previousStatus) {
        VehicleStatusChangedPayload payload = new VehicleStatusChangedPayload(
                vehicle.getId(),
                vehicle.getAgencyId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getModelYear(),
                vehicle.getFuelType().name(),
                vehicle.getTransmission().name(),
                vehicle.getDailyPoolPrice(),
                newStatus.name(),
                previousStatus
        );
        eventProducer.publishStatusChanged(payload, UUID.randomUUID());
    }

    private VehicleDto toDto(Vehicle vehicle) {
        VehicleInspection inspection = vehicle.getInspection();
        return VehicleDto.builder()
                .id(vehicle.getId())
                .agencyId(vehicle.getAgencyId())
                .plate(vehicle.getPlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .modelYear(vehicle.getModelYear())
                .fuelType(vehicle.getFuelType())
                .transmission(vehicle.getTransmission())
                .dailyPoolPrice(vehicle.getDailyPoolPrice())
                .status(vehicle.getStatus())
                .trafficInsuranceExpiry(inspection != null ? inspection.getTrafficInsuranceExpiry() : null)
                .cascoExpiry(inspection != null ? inspection.getCascoExpiry() : null)
                .inspectionExpiry(inspection != null ? inspection.getInspectionExpiry() : null)
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}

