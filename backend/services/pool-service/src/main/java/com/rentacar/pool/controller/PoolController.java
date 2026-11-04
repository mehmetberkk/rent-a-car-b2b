package com.rentacar.pool.controller;

import com.rentacar.common.dto.ApiResponse;
import com.rentacar.common.dto.PageResponse;
import com.rentacar.pool.dto.PoolDtos.*;
import com.rentacar.pool.service.PoolListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pool")
@RequiredArgsConstructor
public class PoolController {

    private final PoolListingService poolListingService;

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<PageResponse<PoolListingDto>>> getPoolListings(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "true") boolean excludeOwn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Agency-Ids", defaultValue = "") String agencyIdsHeader
    ) {
        List<UUID> userAgencyIds = parseAgencyIds(agencyIdsHeader);
        Page<PoolListingDto> result = poolListingService.searchListings(
                city,
                brand,
                fuelType,
                transmission,
                minPrice,
                maxPrice,
                excludeOwn,
                userAgencyIds,
                PageRequest.of(page, size, Sort.by("listedAt").descending())
        );

        PageResponse<PoolListingDto> pageResponse = PageResponse.of(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/listings/{vehicleId}")
    public ResponseEntity<ApiResponse<PoolListingDto>> getListingByVehicleId(
            @PathVariable UUID vehicleId
    ) {
        PoolListingDto listing = poolListingService.getListingByVehicleId(vehicleId);
        return ResponseEntity.ok(ApiResponse.success(listing));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<FilterOptionsDto>> getFilterOptions() {
        FilterOptionsDto filterOptions = poolListingService.getFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(filterOptions));
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

