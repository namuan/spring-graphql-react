package com.showroom.vehicle.dto;

import java.util.UUID;

public record TrimDto(
        UUID id,
        String name,
        long priceDeltaCents) {
}
