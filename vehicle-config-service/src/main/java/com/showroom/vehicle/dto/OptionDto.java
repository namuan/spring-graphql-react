package com.showroom.vehicle.dto;

import com.showroom.vehicle.domain.OptionCategory;

import java.util.UUID;

public record OptionDto(
        UUID id,
        String name,
        OptionCategory category,
        long priceCents) {
}
