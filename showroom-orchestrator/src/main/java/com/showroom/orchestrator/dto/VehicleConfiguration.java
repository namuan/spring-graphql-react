package com.showroom.orchestrator.dto;

import java.util.List;

public record VehicleConfiguration(
        String id,
        String status,
        String createdAt,
        Long totalPriceCents,
        VehicleModel model,
        Trim trim,
        List<VehicleOption> options) {
}
