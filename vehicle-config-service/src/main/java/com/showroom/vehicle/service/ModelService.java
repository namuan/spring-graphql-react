package com.showroom.vehicle.service;

import com.showroom.vehicle.domain.VehicleModel;
import com.showroom.vehicle.dto.ModelDto;
import com.showroom.vehicle.dto.ModelFilter;
import com.showroom.vehicle.error.ApiException;
import com.showroom.vehicle.repository.VehicleModelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ModelService {

    private final VehicleModelRepository modelRepository;
    private final DtoMapper mapper;

    public ModelService(VehicleModelRepository modelRepository, DtoMapper mapper) {
        this.modelRepository = modelRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ModelDto> getAllModels() {
        return getAllModels(null);
    }

    @Transactional(readOnly = true)
    public List<ModelDto> getAllModels(ModelFilter filter) {
        validate(filter);
        return modelRepository.findFiltered(
                        filter == null ? null : lowercasedBrand(filter.brand()),
                        filter == null ? null : filter.minBasePriceCents(),
                        filter == null ? null : filter.maxBasePriceCents(),
                        filter == null ? null : filter.minPowerPs(),
                        filter == null ? null : filter.maxPowerPs(),
                        filter == null ? null : filter.minTopSpeedKph(),
                        filter == null ? null : filter.maxTopSpeedKph(),
                        filter == null ? null : filter.minAccelerationS(),
                        filter == null ? null : filter.maxAccelerationS(),
                        filter == null ? null : filter.minSeats(),
                        filter == null ? null : filter.maxSeats())
                .stream().map(mapper::toModelDto).toList();
    }

    @Transactional(readOnly = true)
    public ModelDto getModel(UUID id) {
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MODEL_NOT_FOUND",
                        "Vehicle model not found for id " + id));
        return mapper.toModelDto(model);
    }

    private static void validate(ModelFilter filter) {
        if (filter == null) {
            return;
        }
        rejectIfInverted(filter.minBasePriceCents(), filter.maxBasePriceCents(), "base price");
        rejectIfInverted(filter.minPowerPs(), filter.maxPowerPs(), "power");
        rejectIfInverted(filter.minTopSpeedKph(), filter.maxTopSpeedKph(), "top speed");
        rejectIfInverted(filter.minAccelerationS(), filter.maxAccelerationS(), "acceleration");
        rejectIfInverted(filter.minSeats(), filter.maxSeats(), "seats");
    }

    private static void rejectIfInverted(Number min, Number max, String label) {
        if (min != null && max != null && min.doubleValue() > max.doubleValue()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDATION_ERROR",
                    "Minimum " + label + " exceeds maximum " + label);
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * The repository compares {@code lower(brand) = :brand}, so the value must
     * arrive lowercased. Applying lower() to the bind parameter itself breaks
     * on PostgreSQL when the parameter is null (it is bound as bytea).
     */
    private static String lowercasedBrand(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }
}
