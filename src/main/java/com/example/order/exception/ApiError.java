package com.example.order.exception;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private Instant timestamp;
    private String path;
    private String code;
    private String message;
    private List<String> details;
}

