package com.rentacar.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {
    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private Instant timestamp;
    private List<InvalidParam> invalidParams;
}

