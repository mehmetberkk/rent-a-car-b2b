package com.rentacar.pool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PoolDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencySummaryDto {
        private UUID id;
        private String name;
        private String city;
        private String phone;
        private String contactPerson;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoolListingDto {
        private UUID id;
        private UUID vehicleId;
        private UUID agencyId;
        private String plate;
        private String brand;
        private String model;
        private Integer modelYear;
        private String fuelType;
        private String transmission;
        private BigDecimal dailyPoolPrice;
        private Instant listedAt;
        private AgencySummaryDto agency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOptionsDto {
        private List<String> cities;
        private List<String> brands;
        private List<String> fuelTypes;
        private List<String> transmissions;
    }
}

