package com.showroom.vehicle.controller;

import com.showroom.vehicle.dto.ModelDto;
import com.showroom.vehicle.dto.ModelFilter;
import com.showroom.vehicle.service.ModelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public List<ModelDto> getAllModels(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minBasePriceCents,
            @RequestParam(required = false) Integer maxBasePriceCents,
            @RequestParam(required = false) Integer minPowerPs,
            @RequestParam(required = false) Integer maxPowerPs,
            @RequestParam(required = false) Integer minTopSpeedKph,
            @RequestParam(required = false) Integer maxTopSpeedKph,
            @RequestParam(required = false) BigDecimal minAccelerationS,
            @RequestParam(required = false) BigDecimal maxAccelerationS,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) Integer maxSeats) {
        return modelService.getAllModels(new ModelFilter(brand, minBasePriceCents, maxBasePriceCents,
                minPowerPs, maxPowerPs, minTopSpeedKph, maxTopSpeedKph,
                minAccelerationS, maxAccelerationS, minSeats, maxSeats));
    }

    @GetMapping("/{id}")
    public ModelDto getModel(@PathVariable UUID id) {
        return modelService.getModel(id);
    }
}
