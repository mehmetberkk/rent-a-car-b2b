package com.rentacar.identity.controller;

import com.rentacar.common.dto.ApiResponse;
import com.rentacar.identity.dto.AgencyDtos.*;
import com.rentacar.identity.service.AgencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateAgencyResponse>> createAgency(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", defaultValue = "") String userEmail,
            @Valid @RequestBody CreateAgencyRequest request
    ) {
        CreateAgencyResponse response = agencyService.createAgency(UUID.fromString(userId), userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dükkan başarıyla oluşturuldu.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgencyDto>>> getAgencies(
            @RequestHeader("X-User-Id") String userId
    ) {
        List<AgencyDto> agencies = agencyService.getAgenciesByOwner(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(agencies));
    }

    @GetMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyDto>> getAgencyById(
            @PathVariable UUID agencyId,
            @RequestHeader("X-User-Id") String userId
    ) {
        AgencyDto agency = agencyService.getAgencyById(agencyId, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(agency));
    }

    @PutMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<AgencyDto>> updateAgency(
            @PathVariable UUID agencyId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateAgencyRequest request
    ) {
        AgencyDto agency = agencyService.updateAgency(agencyId, UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success("Dükkan bilgileri güncellendi.", agency));
    }

    @DeleteMapping("/{agencyId}")
    public ResponseEntity<ApiResponse<DeleteAgencyResponse>> deleteAgency(
            @PathVariable UUID agencyId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", defaultValue = "") String userEmail
    ) {
        DeleteAgencyResponse response = agencyService.deleteAgency(agencyId, UUID.fromString(userId), userEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

