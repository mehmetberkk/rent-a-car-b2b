package com.rentacar.identity.dto;

import com.rentacar.identity.dto.AuthDtos.RefreshedTokenResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class AgencyDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyDto {
        private UUID id;
        private UUID ownerId;
        private String name;
        private String city;
        private String district;
        private String phone;
        private String contactPerson;
        private String address;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAgencyRequest {
        @NotBlank(message = "Dükkan adı boş olamaz")
        @Size(min = 2, max = 200, message = "Dükkan adı 2 ile 200 karakter arasında olmalıdır")
        private String name;

        @NotBlank(message = "Şehir boş olamaz")
        @Size(min = 2, max = 100, message = "Şehir 2 ile 100 karakter arasında olmalıdır")
        private String city;

        @Size(max = 100, message = "İlçe en fazla 100 karakter olabilir")
        private String district;

        @NotBlank(message = "Telefon boş olamaz")
        @Size(min = 10, max = 20, message = "Telefon numarası en az 10 haneli olmalıdır")
        private String phone;

        @NotBlank(message = "Sorumlu kişi boş olamaz")
        @Size(min = 2, max = 200, message = "Sorumlu kişi adı 2 ile 200 karakter arasında olmalıdır")
        private String contactPerson;

        @NotBlank(message = "Adres boş olamaz")
        @Size(min = 10, max = 500, message = "Adres 10 ile 500 karakter arasında olmalıdır")
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAgencyResponse {
        private AgencyDto agency;
        private RefreshedTokenResponse refreshedToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteAgencyResponse {
        private String message;
        private RefreshedTokenResponse refreshedToken;
    }
}

