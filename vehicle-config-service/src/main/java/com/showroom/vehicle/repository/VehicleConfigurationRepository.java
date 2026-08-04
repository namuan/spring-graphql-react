package com.showroom.vehicle.repository;

import com.showroom.vehicle.domain.VehicleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleConfigurationRepository extends JpaRepository<VehicleConfiguration, UUID> {
}
