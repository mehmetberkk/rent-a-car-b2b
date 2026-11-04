package com.rentacar.fleet.dto;

import com.rentacar.fleet.model.Vehicle.FuelType;
import com.rentacar.fleet.model.Vehicle.Transmission;
import com.rentacar.fleet.model.Vehicle.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class VehicleDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVehicleRequest {
        @NotNull(message = "Dükkan seçimi zorunludur")
        private UUID agencyId;

        @NotBlank(message = "Plaka boş olamaz")
        @Size(max = 15, message = "Plaka en fazla 15 karakter olabilir")
        private String plate;

        @NotBlank(message = "Marka boş olamaz")
        @Size(min = 1, max = 50, message = "Marka 1 ile 50 karakter arasında olmalıdır")
        private String brand;

        @NotBlank(message = "Model boş olamaz")
        @Size(min = 1, max = 50, message = "Model 1 ile 50 karakter arasında olmalıdır")
        private String model;

        @NotNull(message = "Model yılı zorunludur")
        @Min(value = 1980, message = "Model yılı 1980 veya sonrası olmalıdır")
        private Integer modelYear;

        @NotNull(message = "Yakıt tipi zorunludur")
        private FuelType fuelType;

        @NotNull(message = "Vites tipi zorunludur")
        private Transmission transmission;

        @NotNull(message = "Günlük kira bedeli zorunludur")
        @DecimalMin(value = "0.01", message = "Günlük kira bedeli 0'dan büyük olmalıdır")
        private BigDecimal dailyPoolPrice;

        private LocalDate trafficInsuranceExpiry;
        private LocalDate cascoExpiry;
        private LocalDate inspectionExpiry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateVehicleRequest {
        @NotBlank(message = "Plaka boş olamaz")
        @Size(max = 15, message = "Plaka en fazla 15 karakter olabilir")
        private String plate;

        @NotBlank(message = "Marka boş olamaz")
        @Size(min = 1, max = 50)
        private String brand;

        @NotBlank(message = "Model boş olamaz")
        @Size(min = 1, max = 50)
        private String model;

        @NotNull(message = "Model yılı zorunludur")
        private Integer modelYear;

        @NotNull(message = "Yakıt tipi zorunludur")
        private FuelType fuelType;

        @NotNull(message = "Vites tipi zorunludur")
        private Transmission transmission;

        @NotNull(message = "Günlük kira bedeli zorunludur")
        @DecimalMin(value = "0.01")
        private BigDecimal dailyPoolPrice;

        private LocalDate trafficInsuranceExpiry;
        private LocalDate cascoExpiry;
        private LocalDate inspectionExpiry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "Yeni durum zorunludur")
        private VehicleStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDto {
        private UUID id;
        private UUID agencyId;
        private String plate;
        private String brand;
        private String model;
        private Integer modelYear;
        private FuelType fuelType;
        private Transmission transmission;
        private BigDecimal dailyPoolPrice;
        private VehicleStatus status;
        private LocalDate trafficInsuranceExpiry;
        private LocalDate cascoExpiry;
        private LocalDate inspectionExpiry;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChangedResponse {
        private UUID id;
        private String plate;
        private VehicleStatus status;
        private VehicleStatus previousStatus;
        private Instant updatedAt;
    }
}

