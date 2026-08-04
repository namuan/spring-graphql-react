package com.showroom.vehicle.repository;

import com.showroom.vehicle.domain.Trim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrimRepository extends JpaRepository<Trim, UUID> {
}
