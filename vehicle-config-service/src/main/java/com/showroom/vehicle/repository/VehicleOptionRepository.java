package com.showroom.vehicle.repository;

import com.showroom.vehicle.domain.VehicleOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleOptionRepository extends JpaRepository<VehicleOption, UUID> {
}
