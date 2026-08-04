package com.showroom.vehicle.service;

import com.showroom.vehicle.domain.ConfigurationStatus;
import com.showroom.vehicle.domain.Trim;
import com.showroom.vehicle.domain.VehicleConfiguration;
import com.showroom.vehicle.domain.VehicleModel;
import com.showroom.vehicle.domain.VehicleOption;
import com.showroom.vehicle.dto.ConfigurationDto;
import com.showroom.vehicle.dto.ConfigurationRequest;
import com.showroom.vehicle.error.ApiException;
import com.showroom.vehicle.repository.TrimRepository;
import com.showroom.vehicle.repository.VehicleConfigurationRepository;
import com.showroom.vehicle.repository.VehicleModelRepository;
import com.showroom.vehicle.repository.VehicleOptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConfigurationService {

    private final VehicleModelRepository modelRepository;
    private final TrimRepository trimRepository;
    private final VehicleOptionRepository optionRepository;
    private final VehicleConfigurationRepository configurationRepository;
    private final DtoMapper mapper;

    public ConfigurationService(VehicleModelRepository modelRepository,
                                TrimRepository trimRepository,
                                VehicleOptionRepository optionRepository,
                                VehicleConfigurationRepository configurationRepository,
                                DtoMapper mapper) {
        this.modelRepository = modelRepository;
        this.trimRepository = trimRepository;
        this.optionRepository = optionRepository;
        this.configurationRepository = configurationRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ConfigurationDto create(ConfigurationRequest request) {
        VehicleModel model = modelRepository.findById(request.modelId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "MODEL_NOT_FOUND",
                        "Vehicle model not found for id " + request.modelId()));

        Trim trim = trimRepository.findById(request.trimId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "TRIM_NOT_FOUND",
                        "Trim not found for id " + request.trimId()));

        if (!trim.getModel().getId().equals(model.getId())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "TRIM_NOT_FOR_MODEL",
                    "Trim " + trim.getId() + " does not belong to model " + model.getId());
        }

        List<UUID> requestedOptionIds = request.optionIds() == null ? List.of() : request.optionIds();
        Set<UUID> uniqueOptionIds = new LinkedHashSet<>(requestedOptionIds);
        if (uniqueOptionIds.size() != requestedOptionIds.size()) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "DUPLICATE_OPTION_IDS",
                    "Duplicate option ids are not allowed");
        }

        List<VehicleOption> options = optionRepository.findAllById(uniqueOptionIds);
        if (options.size() != uniqueOptionIds.size()) {
            Set<UUID> foundIds = options.stream().map(VehicleOption::getId).collect(java.util.stream.Collectors.toSet());
            UUID missingId = uniqueOptionIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "OPTION_NOT_FOUND",
                    "Vehicle option not found for id " + missingId);
        }

        for (VehicleOption option : options) {
            if (!option.getModel().getId().equals(model.getId())) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "OPTION_NOT_FOR_MODEL",
                        "Option " + option.getId() + " does not belong to model " + model.getId());
            }
        }

        long totalPriceCents = model.getBasePriceCents()
                + trim.getPriceDeltaCents()
                + options.stream().mapToLong(VehicleOption::getPriceCents).sum();

        VehicleConfiguration configuration = new VehicleConfiguration();
        configuration.setId(UUID.randomUUID());
        configuration.setModel(model);
        configuration.setTrim(trim);
        configuration.setOptions(new LinkedHashSet<>(options));
        configuration.setStatus(ConfigurationStatus.DRAFT);
        configuration.setCreatedAt(Instant.now());
        configuration.setTotalPriceCents(totalPriceCents);

        configurationRepository.save(configuration);
        return mapper.toConfigurationDto(configuration);
    }

    @Transactional(readOnly = true)
    public ConfigurationDto getConfiguration(UUID id) {
        VehicleConfiguration configuration = configurationRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "CONFIGURATION_NOT_FOUND",
                        "Vehicle configuration not found for id " + id));
        return mapper.toConfigurationDto(configuration);
    }
}
