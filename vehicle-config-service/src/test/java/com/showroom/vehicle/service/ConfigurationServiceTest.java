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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationServiceTest {

    private static final UUID VALE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TERRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TOURING_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID PANORAMIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID VELOCITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");

    private VehicleModelRepository modelRepository;
    private TrimRepository trimRepository;
    private VehicleOptionRepository optionRepository;
    private VehicleConfigurationRepository configurationRepository;
    private ConfigurationService service;

    @BeforeEach
    void setUp() {
        modelRepository = mock(VehicleModelRepository.class);
        trimRepository = mock(TrimRepository.class);
        optionRepository = mock(VehicleOptionRepository.class);
        configurationRepository = mock(VehicleConfigurationRepository.class);
        service = new ConfigurationService(modelRepository, trimRepository, optionRepository,
                configurationRepository, new DtoMapper());
    }

    @Test
    void createConfigurationCalculatesTotalPriceAndPersists() {
        VehicleModel vale = model(VALE_ID);
        Trim touring = trim(TOURING_ID, vale, 0);
        VehicleOption panoramic = option(PANORAMIC_ID, vale, 145_000);
        VehicleOption velocity = option(VELOCITY_ID, vale, 265_000);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(touring));
        when(optionRepository.findAllById(any())).thenReturn(List.of(panoramic, velocity));

        ConfigurationDto dto = service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of(PANORAMIC_ID, VELOCITY_ID)));

        assertThat(dto.totalPriceCents()).isEqualTo(5_000_000);
        assertThat(dto.status()).isEqualTo(ConfigurationStatus.DRAFT);
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.model().id()).isEqualTo(VALE_ID);
        assertThat(dto.trim().name()).isEqualTo("Touring");
        assertThat(dto.options()).hasSize(2);

        ArgumentCaptor<VehicleConfiguration> captor = ArgumentCaptor.forClass(VehicleConfiguration.class);
        verify(configurationRepository).save(captor.capture());
        VehicleConfiguration saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ConfigurationStatus.DRAFT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTotalPriceCents()).isEqualTo(5_000_000);
        assertThat(saved.getOptions()).hasSize(2);
    }

    @Test
    void createAllowsConfigurationWithoutOptions() {
        VehicleModel vale = model(VALE_ID);
        Trim touring = trim(TOURING_ID, vale, 0);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(touring));

        ConfigurationDto dto = service.create(new ConfigurationRequest(VALE_ID, TOURING_ID, List.of()));

        assertThat(dto.totalPriceCents()).isEqualTo(4_590_000);
        assertThat(dto.options()).isEmpty();

        ArgumentCaptor<VehicleConfiguration> captor = ArgumentCaptor.forClass(VehicleConfiguration.class);
        verify(configurationRepository).save(captor.capture());
        assertThat(captor.getValue().getOptions()).isEmpty();
    }

    @Test
    void createThrowsModelNotFound() {
        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getErrorCode()).isEqualTo("MODEL_NOT_FOUND");
                });

        verify(configurationRepository, never()).save(any());
    }

    @Test
    void createThrowsTrimNotFound() {
        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(model(VALE_ID)));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getErrorCode()).isEqualTo("TRIM_NOT_FOUND");
                });
    }

    @Test
    void createRejectsTrimThatBelongsToAnotherModel() {
        VehicleModel vale = model(VALE_ID);
        VehicleModel terra = model(TERRA_ID);
        Trim terraTrim = trim(TOURING_ID, terra, 0);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(terraTrim));

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(422);
                    assertThat(api.getErrorCode()).isEqualTo("TRIM_NOT_FOR_MODEL");
                });
    }

    @Test
    void createRejectsDuplicateOptionIds() {
        VehicleModel vale = model(VALE_ID);
        Trim touring = trim(TOURING_ID, vale, 0);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(touring));

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of(PANORAMIC_ID, PANORAMIC_ID))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(422);
                    assertThat(api.getErrorCode()).isEqualTo("DUPLICATE_OPTION_IDS");
                });

        verify(optionRepository, never()).findAllById(any());
        verify(configurationRepository, never()).save(any());
    }

    @Test
    void createThrowsOptionNotFound() {
        VehicleModel vale = model(VALE_ID);
        Trim touring = trim(TOURING_ID, vale, 0);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(touring));
        when(optionRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of(PANORAMIC_ID))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getErrorCode()).isEqualTo("OPTION_NOT_FOUND");
                });
    }

    @Test
    void createRejectsOptionThatBelongsToAnotherModel() {
        VehicleModel vale = model(VALE_ID);
        VehicleModel terra = model(TERRA_ID);
        Trim touring = trim(TOURING_ID, vale, 0);
        VehicleOption terraOption = option(PANORAMIC_ID, terra, 210_000);

        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(vale));
        when(trimRepository.findById(TOURING_ID)).thenReturn(Optional.of(touring));
        when(optionRepository.findAllById(any())).thenReturn(List.of(terraOption));

        assertThatThrownBy(() -> service.create(
                new ConfigurationRequest(VALE_ID, TOURING_ID, List.of(PANORAMIC_ID))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(422);
                    assertThat(api.getErrorCode()).isEqualTo("OPTION_NOT_FOR_MODEL");
                });
    }

    @Test
    void getConfigurationMapsNestedDto() {
        VehicleModel vale = model(VALE_ID);
        Trim touring = trim(TOURING_ID, vale, 0);
        VehicleOption panoramic = option(PANORAMIC_ID, vale, 145_000);

        VehicleConfiguration configuration = new VehicleConfiguration();
        configuration.setId(UUID.fromString("00000000-0000-0000-0000-000000000031"));
        configuration.setModel(vale);
        configuration.setTrim(touring);
        configuration.setOptions(new java.util.LinkedHashSet<>(List.of(panoramic)));
        configuration.setStatus(ConfigurationStatus.DRAFT);
        configuration.setCreatedAt(java.time.Instant.parse("2026-01-02T03:04:05Z"));
        configuration.setTotalPriceCents(4_735_000);

        when(configurationRepository.findById(configuration.getId())).thenReturn(Optional.of(configuration));

        ConfigurationDto dto = service.getConfiguration(configuration.getId());

        assertThat(dto.id()).isEqualTo(configuration.getId());
        assertThat(dto.status()).isEqualTo(ConfigurationStatus.DRAFT);
        assertThat(dto.createdAt()).isEqualTo(java.time.Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(dto.totalPriceCents()).isEqualTo(4_735_000);
        assertThat(dto.model().id()).isEqualTo(VALE_ID);
        assertThat(dto.trim().name()).isEqualTo("Touring");
        assertThat(dto.options()).hasSize(1);
        assertThat(dto.options().getFirst().name()).isEqualTo("Panoramic canopy");
    }

    @Test
    void getConfigurationThrowsNotFound() {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(configurationRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConfiguration(unknownId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getErrorCode()).isEqualTo("CONFIGURATION_NOT_FOUND");
                });
    }

    private static VehicleModel model(UUID id) {
        VehicleModel model = new VehicleModel();
        model.setId(id);
        model.setBrand("Aster");
        model.setName(id.equals(VALE_ID) ? "Vale" : "Terra");
        model.setModelYear(2026);
        model.setBasePriceCents(id.equals(VALE_ID) ? 4_590_000 : 5_290_000);
        model.setDescription("Test model");
        model.setTrims(new ArrayList<>());
        model.setOptions(new ArrayList<>());
        return model;
    }

    private static Trim trim(UUID id, VehicleModel model, long delta) {
        Trim trim = new Trim();
        trim.setId(id);
        trim.setModel(model);
        trim.setName("Touring");
        trim.setPriceDeltaCents(delta);
        return trim;
    }

    private static VehicleOption option(UUID id, VehicleModel model, long price) {
        VehicleOption option = new VehicleOption();
        option.setId(id);
        option.setModel(model);
        option.setName("Panoramic canopy");
        option.setCategory(com.showroom.vehicle.domain.OptionCategory.EXTERIOR);
        option.setPriceCents(price);
        return option;
    }
}
