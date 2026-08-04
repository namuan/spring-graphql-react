package com.showroom.vehicle.service;

import com.showroom.vehicle.domain.OptionCategory;
import com.showroom.vehicle.domain.Trim;
import com.showroom.vehicle.domain.VehicleModel;
import com.showroom.vehicle.domain.VehicleOption;
import com.showroom.vehicle.dto.ModelDto;
import com.showroom.vehicle.dto.ModelFilter;
import com.showroom.vehicle.error.ApiException;
import com.showroom.vehicle.repository.VehicleModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelServiceTest {

    private static final UUID VALE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private VehicleModelRepository modelRepository;
    private ModelService service;

    @BeforeEach
    void setUp() {
        modelRepository = mock(VehicleModelRepository.class);
        service = new ModelService(modelRepository, new DtoMapper());
    }

    @Test
    void getAllModelsMapsFullDtosWithTrimsAndOptions() {
        VehicleModel vale = model(VALE_ID);
        VehicleModel terra = model(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        when(modelRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(vale, terra));

        List<ModelDto> dtos = service.getAllModels();

        assertThat(dtos).hasSize(2);
        ModelDto valeDto = dtos.getFirst();
        assertThat(valeDto.brand()).isEqualTo("Aster");
        assertThat(valeDto.name()).isEqualTo("Vale");
        assertThat(valeDto.modelYear()).isEqualTo(2026);
        assertThat(valeDto.basePriceCents()).isEqualTo(4_590_000);
        assertThat(valeDto.description()).isEqualTo("Test model");
        assertThat(valeDto.engine()).isEqualTo("Twin-turbo V6");
        assertThat(valeDto.powerPs()).isEqualTo(520);
        assertThat(valeDto.accelerationS()).isEqualByComparingTo("3.9");
        assertThat(valeDto.topSpeedKph()).isEqualTo(300);
        assertThat(valeDto.drivetrain()).isEqualTo("AWD");
        assertThat(valeDto.rangeKm()).isNull();
        assertThat(valeDto.seats()).isEqualTo(4);
        assertThat(valeDto.trims()).hasSize(1);
        assertThat(valeDto.trims().getFirst().name()).isEqualTo("Touring");
        assertThat(valeDto.trims().getFirst().priceDeltaCents()).isZero();
        assertThat(valeDto.options()).hasSize(1);
        assertThat(valeDto.options().getFirst().name()).isEqualTo("Panoramic canopy");
        assertThat(valeDto.options().getFirst().category()).isEqualTo(OptionCategory.EXTERIOR);
        assertThat(valeDto.options().getFirst().priceCents()).isEqualTo(145_000);
    }

    @Test
    void getAllModelsPassesFilterToRepository() {
        when(modelRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(model(VALE_ID)));

        ModelFilter filter = new ModelFilter("Aster", null, 6_000_000, 500, null,
                null, null, null, null, null, null);
        List<ModelDto> dtos = service.getAllModels(filter);

        assertThat(dtos).hasSize(1);

        ArgumentCaptor<String> brandCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> minPowerCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> maxPriceCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(modelRepository).findFiltered(brandCaptor.capture(), any(), maxPriceCaptor.capture(),
                minPowerCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(brandCaptor.getValue()).isEqualTo("aster");
        assertThat(maxPriceCaptor.getValue()).isEqualTo(6_000_000);
        assertThat(minPowerCaptor.getValue()).isEqualTo(500);
    }

    @Test
    void getAllModelsNormalizesBlankBrandToNull() {
        when(modelRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(model(VALE_ID)));

        ModelFilter filter = new ModelFilter("  ", null, null, null, null,
                null, null, null, null, null, null);
        service.getAllModels(filter);

        ArgumentCaptor<String> brandCaptor = ArgumentCaptor.forClass(String.class);
        verify(modelRepository).findFiltered(brandCaptor.capture(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());
        assertThat(brandCaptor.getValue()).isNull();
    }

    @Test
    void getAllModelsRejectsInvertedRanges() {
        ModelFilter filter = new ModelFilter(null, null, null, 600, 100,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> service.getAllModels(filter))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(422);
                    assertThat(api.getErrorCode()).isEqualTo("VALIDATION_ERROR");
                });
    }

    @Test
    void getModelMapsDto() {
        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.of(model(VALE_ID)));

        ModelDto dto = service.getModel(VALE_ID);

        assertThat(dto.id()).isEqualTo(VALE_ID);
        assertThat(dto.name()).isEqualTo("Vale");
        assertThat(dto.trims()).hasSize(1);
        assertThat(dto.options()).hasSize(1);
    }

    @Test
    void getModelThrowsNotFound() {
        when(modelRepository.findById(VALE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getModel(VALE_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getErrorCode()).isEqualTo("MODEL_NOT_FOUND");
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
        model.setEngine("Twin-turbo V6");
        model.setPowerPs(520);
        model.setAccelerationS(new java.math.BigDecimal("3.9"));
        model.setTopSpeedKph(300);
        model.setDrivetrain("AWD");
        model.setRangeKm(null);
        model.setSeats(4);

        Trim touring = new Trim();
        touring.setId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
        touring.setModel(model);
        touring.setName("Touring");
        touring.setPriceDeltaCents(0);

        VehicleOption panoramic = new VehicleOption();
        panoramic.setId(UUID.fromString("00000000-0000-0000-0000-000000000021"));
        panoramic.setModel(model);
        panoramic.setName("Panoramic canopy");
        panoramic.setCategory(OptionCategory.EXTERIOR);
        panoramic.setPriceCents(145_000);

        model.setTrims(new ArrayList<>(List.of(touring)));
        model.setOptions(new ArrayList<>(List.of(panoramic)));
        return model;
    }
}
