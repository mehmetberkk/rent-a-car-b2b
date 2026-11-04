package com.rentacar.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidParam {
    private String name;
    private String reason;
    private Object rejectedValue;
}

