package com.showroom.vehicle.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ModelDto(
        UUID id,
        String brand,
        String name,
        int modelYear,
        long basePriceCents,
        String description,
        String engine,
        int powerPs,
        BigDecimal accelerationS,
        int topSpeedKph,
        String drivetrain,
        Integer rangeKm,
        int seats,
        List<TrimDto> trims,
        List<OptionDto> options) {
}
