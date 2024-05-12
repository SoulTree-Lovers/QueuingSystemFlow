package com.example.queuingsystemflow.dto;

public record AllowUserResponse(
    Long requestCount,
    Long allowedCount
) {
}
