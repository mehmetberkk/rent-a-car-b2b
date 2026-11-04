package com.rentacar.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class AuthDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Ad alanı boş bırakılamaz")
        @Size(min = 2, max = 100, message = "Ad 2 ile 100 karakter arasında olmalıdır")
        private String firstName;

        @NotBlank(message = "Soyad alanı boş bırakılamaz")
        @Size(min = 2, max = 100, message = "Soyad 2 ile 100 karakter arasında olmalıdır")
        private String lastName;

        @NotBlank(message = "E-posta alanı boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        @Size(max = 255, message = "E-posta en fazla 255 karakter olabilir")
        private String email;

        @NotBlank(message = "Şifre alanı boş bırakılamaz")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Şifre en az 8 karakter uzunluğunda olmalı, en az bir büyük harf, bir küçük harf ve bir rakam içermelidir"
        )
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "E-posta alanı boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        private String email;

        @NotBlank(message = "Şifre alanı boş bırakılamaz")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivateRequest {
        @NotBlank(message = "Aktivasyon jetonu boş olamaz")
        private String token;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResendActivationRequest {
        @NotBlank(message = "E-posta alanı boş bırakılamaz")
        @Email(message = "Geçerli bir e-posta adresi giriniz")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserDto user;
        private List<UUID> agencyIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshedTokenResponse {
        private String accessToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private long expiresIn;
        private List<UUID> agencyIds;
    }
}

