package com.showroom.orchestrator.dto;

import java.math.BigDecimal;

/**
 * Optional filter passed through to the downstream catalogue query. Field
 * names match the GraphQL {@code ModelFilter} input exactly; nulls mean the
 * downstream side imposes no constraint.
 */
public record ModelFilter(
        String brand,
        Integer minBasePriceCents,
        Integer maxBasePriceCents,
        Integer minPowerPs,
        Integer maxPowerPs,
        Integer minTopSpeedKph,
        Integer maxTopSpeedKph,
        BigDecimal minAccelerationS,
        BigDecimal maxAccelerationS,
        Integer minSeats,
        Integer maxSeats) {
}
