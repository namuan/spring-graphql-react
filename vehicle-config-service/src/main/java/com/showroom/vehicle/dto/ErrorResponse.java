package com.showroom.vehicle.dto;

public record ErrorResponse(
        String errorCode,
        String message) {
}
