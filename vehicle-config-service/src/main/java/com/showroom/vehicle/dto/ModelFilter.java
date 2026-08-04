package com.showroom.vehicle.dto;

import java.math.BigDecimal;

/**
 * Optional criteria for the model catalogue. Every field is nullable; a null
 * bound means "no constraint on that side". Numeric bounds are inclusive.
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
