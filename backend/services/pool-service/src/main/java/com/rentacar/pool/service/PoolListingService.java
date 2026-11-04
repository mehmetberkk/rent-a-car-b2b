package com.rentacar.pool.service;

import com.rentacar.pool.dto.PoolDtos.*;
import com.rentacar.pool.model.PoolListing;
import com.rentacar.pool.repository.PoolListingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoolListingService {

    private final PoolListingRepository poolListingRepository;

    @Transactional(readOnly = true)
    public Page<PoolListingDto> searchListings(
            String city,
            String brand,
            String fuelType,
            String transmission,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean excludeOwn,
            List<UUID> userAgencyIds,
            Pageable pageable
    ) {
        Specification<PoolListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("agencyCity")), city.trim().toLowerCase()));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
            }

            if (fuelType != null && !fuelType.isBlank()) {
                predicates.add(cb.equal(root.get("fuelType"), fuelType.trim().toUpperCase()));
            }

            if (transmission != null && !transmission.isBlank()) {
                predicates.add(cb.equal(root.get("transmission"), transmission.trim().toUpperCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dailyPoolPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dailyPoolPrice"), maxPrice));
            }

            if (excludeOwn && userAgencyIds != null && !userAgencyIds.isEmpty()) {
                predicates.add(cb.not(root.get("agencyId").in(userAgencyIds)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return poolListingRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PoolListingDto getListingByVehicleId(UUID vehicleId) {
        PoolListing listing = poolListingRepository.findByVehicleId(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Havuzda araç bulunamadı: " + vehicleId));
        return toDto(listing);
    }

    @Transactional(readOnly = true)
    public FilterOptionsDto getFilterOptions() {
        return FilterOptionsDto.builder()
                .cities(poolListingRepository.findDistinctCities())
                .brands(poolListingRepository.findDistinctBrands())
                .fuelTypes(poolListingRepository.findDistinctFuelTypes())
                .transmissions(poolListingRepository.findDistinctTransmissions())
                .build();
    }

    private PoolListingDto toDto(PoolListing listing) {
        AgencySummaryDto agencyDto = AgencySummaryDto.builder()
                .id(listing.getAgencyId())
                .name(listing.getAgencyName())
                .city(listing.getAgencyCity())
                .phone(listing.getAgencyPhone())
                .contactPerson(listing.getContactPerson())
                .address(listing.getAgencyAddress())
                .build();

        return PoolListingDto.builder()
                .id(listing.getId())
                .vehicleId(listing.getVehicleId())
                .agencyId(listing.getAgencyId())
                .plate(listing.getPlate())
                .brand(listing.getBrand())
                .model(listing.getModel())
                .modelYear(listing.getModelYear())
                .fuelType(listing.getFuelType())
                .transmission(listing.getTransmission())
                .dailyPoolPrice(listing.getDailyPoolPrice())
                .listedAt(listing.getListedAt())
                .agency(agencyDto)
                .build();
    }
}

