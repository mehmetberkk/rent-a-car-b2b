package com.rentacar.pool.exception;

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
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("bulunamadı")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:" + (status == HttpStatus.NOT_FOUND ? "not-found" : "invalid-argument")))
                .title(status == HttpStatus.NOT_FOUND ? "Kayıt Bulunamadı" : "Geçersiz İstek")
                .status(status.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred on path {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:internal-server-error"))
                .title("Sunucu Hatası")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyiniz.")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private InvalidParam mapFieldError(FieldError fieldError) {
        return new InvalidParam(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : null
        );
    }
}

