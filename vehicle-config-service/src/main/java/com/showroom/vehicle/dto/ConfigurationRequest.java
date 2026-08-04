package com.showroom.vehicle.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfigurationRequest(
        @NotNull(message = "must not be null") UUID modelId,
        @NotNull(message = "must not be null") UUID trimId,
        List<@NotNull(message = "option ID must not be null") UUID> optionIds) {
}
