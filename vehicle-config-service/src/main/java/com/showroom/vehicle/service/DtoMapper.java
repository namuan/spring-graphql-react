package com.showroom.vehicle.service;

import com.showroom.vehicle.domain.VehicleConfiguration;
import com.showroom.vehicle.domain.VehicleModel;
import com.showroom.vehicle.domain.VehicleOption;
import com.showroom.vehicle.domain.Trim;
import com.showroom.vehicle.dto.ConfigurationDto;
import com.showroom.vehicle.dto.ModelDto;
import com.showroom.vehicle.dto.OptionDto;
import com.showroom.vehicle.dto.TrimDto;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public ModelDto toModelDto(VehicleModel model) {
        return new ModelDto(
                model.getId(),
                model.getBrand(),
                model.getName(),
                model.getModelYear(),
                model.getBasePriceCents(),
                model.getDescription(),
                model.getEngine(),
                model.getPowerPs(),
                model.getAccelerationS(),
                model.getTopSpeedKph(),
                model.getDrivetrain(),
                model.getRangeKm(),
                model.getSeats(),
                model.getTrims().stream().map(this::toTrimDto).toList(),
                model.getOptions().stream().map(this::toOptionDto).toList());
    }

    public TrimDto toTrimDto(Trim trim) {
        return new TrimDto(trim.getId(), trim.getName(), trim.getPriceDeltaCents());
    }

    public OptionDto toOptionDto(VehicleOption option) {
        return new OptionDto(option.getId(), option.getName(), option.getCategory(), option.getPriceCents());
    }

    public ConfigurationDto toConfigurationDto(VehicleConfiguration configuration) {
        return new ConfigurationDto(
                configuration.getId(),
                configuration.getStatus(),
                configuration.getCreatedAt(),
                configuration.getTotalPriceCents(),
                toModelDto(configuration.getModel()),
                toTrimDto(configuration.getTrim()),
                configuration.getOptions().stream().map(this::toOptionDto).toList());
    }
}
