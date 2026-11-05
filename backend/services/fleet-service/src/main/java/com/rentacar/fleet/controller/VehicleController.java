package com.rentacar.fleet.controller;

import com.rentacar.common.dto.ApiResponse;
import com.rentacar.common.dto.PageResponse;
import com.rentacar.fleet.dto.VehicleDtos.*;
import com.rentacar.fleet.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleDto>> createVehicle(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        VehicleDto response = vehicleService.createVehicle(request, allowedAgencyIds);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Araç başarıyla eklendi.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VehicleDto>>> getVehicles(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @RequestParam UUID agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        Page<VehicleDto> vehiclePage = vehicleService.getVehiclesByAgency(
                agencyId,
                allowedAgencyIds,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        PageResponse<VehicleDto> pageResponse = PageResponse.<VehicleDto>builder()
                .content(vehiclePage.getContent())
                .page(vehiclePage.getNumber())
                .size(vehiclePage.getSize())
                .totalElements(vehiclePage.getTotalElements())
                .totalPages(vehiclePage.getTotalPages())
                .first(vehiclePage.isFirst())
                .last(vehiclePage.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleDto>> getVehicleById(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @PathVariable UUID vehicleId
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        VehicleDto response = vehicleService.getVehicleById(vehicleId, allowedAgencyIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleDto>> updateVehicle(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        VehicleDto response = vehicleService.updateVehicle(vehicleId, request, allowedAgencyIds);
        return ResponseEntity.ok(ApiResponse.success("Araç bilgileri güncellendi.", response));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @PathVariable UUID vehicleId
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        vehicleService.deleteVehicle(vehicleId, allowedAgencyIds);
        return ResponseEntity.ok(ApiResponse.success("Araç başarıyla silindi.", null));
    }

    @PatchMapping("/{vehicleId}/status")
    public ResponseEntity<ApiResponse<StatusChangedResponse>> updateVehicleStatus(
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        List<UUID> allowedAgencyIds = parseAgencyIds(agencyIdsHeader);
        StatusChangedResponse response = vehicleService.updateStatus(vehicleId, request.getStatus(), allowedAgencyIds);
        return ResponseEntity.ok(ApiResponse.success("Araç durumu başarıyla güncellendi.", response));
    }

    private List<UUID> parseAgencyIds(String agencyIdsHeader) {
        if (agencyIdsHeader == null || agencyIdsHeader.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(agencyIdsHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
    }
}

