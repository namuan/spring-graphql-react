package com.showroom.orchestrator.dto;

import java.math.BigDecimal;
import java.util.List;

public record VehicleModel(
        String id,
        String brand,
        String name,
        Integer modelYear,
        Long basePriceCents,
        String description,
        String engine,
        Integer powerPs,
        BigDecimal accelerationS,
        Integer topSpeedKph,
        String drivetrain,
        Integer rangeKm,
        Integer seats,
        List<Trim> trims,
        List<VehicleOption> options) {
}
