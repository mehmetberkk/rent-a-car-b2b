package com.rentacar.identity.exception;

import com.rentacar.common.dto.InvalidParam;
import com.rentacar.common.dto.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<InvalidParam> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:validation-error"))
                .title("Doğrulama Hatası")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Gönderilen istek verileri doğrulama kurallarına uymuyor.")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .invalidParams(invalidParams)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:invalid-argument"))
                .title("Geçersiz Parametre")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetails> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:conflict"))
                .title("Durum Uyuşmazlığı")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetails> handleSecurity(SecurityException ex, HttpServletRequest request) {
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:unauthorized"))
                .title("Yetkisiz Erişim")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error: ", ex);
        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:internal-server-error"))
                .title("Sunucu Hatası")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("Beklenmeyen bir sunucu hatası oluştu.")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private InvalidParam mapFieldError(FieldError fieldError) {
        return InvalidParam.builder()
                .name(fieldError.getField())
                .reason(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
}

