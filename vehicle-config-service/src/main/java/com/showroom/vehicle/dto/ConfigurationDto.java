package com.showroom.vehicle.dto;

import com.showroom.vehicle.domain.ConfigurationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConfigurationDto(
        UUID id,
        ConfigurationStatus status,
        Instant createdAt,
        long totalPriceCents,
        ModelDto model,
        TrimDto trim,
        List<OptionDto> options) {
}
