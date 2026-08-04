package com.showroom.vehicle.repository;

import com.showroom.vehicle.domain.VehicleModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, UUID> {

    /**
     * Catalogue search with fully optional, inclusive bounds. Every predicate
     * is a no-op when its parameter is null, so a single query serves both the
     * unfiltered and the filtered cases.
     */
    @Query("""
            SELECT m FROM VehicleModel m
            WHERE (:brand IS NULL OR lower(m.brand) = :brand)
              AND (:minBasePriceCents IS NULL OR m.basePriceCents >= :minBasePriceCents)
              AND (:maxBasePriceCents IS NULL OR m.basePriceCents <= :maxBasePriceCents)
              AND (:minPowerPs IS NULL OR m.powerPs >= :minPowerPs)
              AND (:maxPowerPs IS NULL OR m.powerPs <= :maxPowerPs)
              AND (:minTopSpeedKph IS NULL OR m.topSpeedKph >= :minTopSpeedKph)
              AND (:maxTopSpeedKph IS NULL OR m.topSpeedKph <= :maxTopSpeedKph)
              AND (:minAccelerationS IS NULL OR m.accelerationS >= :minAccelerationS)
              AND (:maxAccelerationS IS NULL OR m.accelerationS <= :maxAccelerationS)
              AND (:minSeats IS NULL OR m.seats >= :minSeats)
              AND (:maxSeats IS NULL OR m.seats <= :maxSeats)
            """)
    List<VehicleModel> findFiltered(
            @Param("brand") String brand,
            @Param("minBasePriceCents") Integer minBasePriceCents,
            @Param("maxBasePriceCents") Integer maxBasePriceCents,
            @Param("minPowerPs") Integer minPowerPs,
            @Param("maxPowerPs") Integer maxPowerPs,
            @Param("minTopSpeedKph") Integer minTopSpeedKph,
            @Param("maxTopSpeedKph") Integer maxTopSpeedKph,
            @Param("minAccelerationS") BigDecimal minAccelerationS,
            @Param("maxAccelerationS") BigDecimal maxAccelerationS,
            @Param("minSeats") Integer minSeats,
            @Param("maxSeats") Integer maxSeats);
}
