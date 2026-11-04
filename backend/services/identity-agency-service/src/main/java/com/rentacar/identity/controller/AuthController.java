package com.rentacar.identity.controller;

import com.rentacar.common.dto.ApiResponse;
import com.rentacar.identity.dto.AuthDtos.*;
import com.rentacar.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Aktivasyon e-postası gönderildi. Lütfen e-postanızı kontrol edin.", user));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Map<String, String>>> activate(@Valid @RequestBody ActivateRequest request) {
        authService.activate(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Hesabınız başarıyla aktive edildi.", Map.of("status", "ACTIVE")));
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<ApiResponse<Void>> resendActivation(@Valid @RequestBody ResendActivationRequest request) {
        authService.resendActivation(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Yeni aktivasyon e-postası gönderildi.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Giriş başarılı.", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshedTokenResponse>> refresh(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        RefreshedTokenResponse response = authService.refresh(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Oturum yenilendi.", response));
    }
}

