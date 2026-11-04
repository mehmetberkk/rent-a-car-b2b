package com.rentacar.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.common.dto.ProblemDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final SecretKey secretKey;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret, ObjectMapper objectMapper) {
        super(Config.class);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Yetkilendirme başlığı eksik veya geçersiz");
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                @SuppressWarnings("unchecked")
                List<String> agencyIds = claims.get("agencyIds", List.class);
                String agencyIdsHeader = agencyIds != null ? String.join(",", agencyIds) : "";

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-Agency-Ids", agencyIdsHeader)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (JwtException e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Geçersiz veya süresi dolmuş oturum anahtarı: " + e.getMessage());
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create("urn:rentacar:problem:auth:unauthorized"))
                .title("Yetkisiz Erişim")
                .status(status.value())
                .detail(message)
                .instance(URI.create(exchange.getRequest().getPath().value()))
                .timestamp(Instant.now())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":401,\"detail\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}

