package com.showroom.vehicle.controller;

import com.showroom.vehicle.dto.ModelDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModelApiIntegrationTest {

    private static final UUID VALE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TERRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Test
    void getAllModelsReturnsFullCatalog() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/models"))
                .andExpect(status().isOk())
                .andReturn();

        List<ModelDto> models = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertThat(models).hasSize(2);
        ModelDto vale = models.stream().filter(m -> m.name().equals("Vale")).findFirst().orElseThrow();
        assertThat(vale.id()).isEqualTo(VALE_ID);
        assertThat(vale.brand()).isEqualTo("Aster");
        assertThat(vale.modelYear()).isEqualTo(2026);
        assertThat(vale.basePriceCents()).isEqualTo(4_590_000);
        assertThat(vale.engine()).isEqualTo("Twin-turbo V6");
        assertThat(vale.powerPs()).isEqualTo(520);
        assertThat(vale.accelerationS()).isEqualByComparingTo("3.9");
        assertThat(vale.topSpeedKph()).isEqualTo(300);
        assertThat(vale.drivetrain()).isEqualTo("AWD");
        assertThat(vale.rangeKm()).isNull();
        assertThat(vale.seats()).isEqualTo(4);
        assertThat(vale.trims()).extracting("name")
                .containsExactlyInAnyOrder("Touring", "Apex");
        assertThat(vale.options()).extracting("name")
                .containsExactlyInAnyOrder("Panoramic canopy", "Velocity package",
                        "Comfort seats", "Infotainment pro");

        ModelDto terra = models.stream().filter(m -> m.name().equals("Terra")).findFirst().orElseThrow();
        assertThat(terra.id()).isEqualTo(TERRA_ID);
        assertThat(terra.trims()).extracting("name")
                .containsExactlyInAnyOrder("Expedition", "Pioneer");
    }

    @Test
    void getAllModelsFiltersByBrandCaseInsensitively() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/models").param("brand", "aster"))
                .andExpect(status().isOk())
                .andReturn();

        List<ModelDto> models = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertThat(models).hasSize(2);
        assertThat(models).allSatisfy(model -> assertThat(model.brand()).isEqualTo("Aster"));
    }

    @Test
    void getAllModelsFiltersByPriceRange() throws Exception {
        // Terra (5,290,000) is inside the range; Vale (4,590,000) is below it.
        MvcResult result = mockMvc.perform(get("/api/models")
                        .param("minBasePriceCents", "5000000")
                        .param("maxBasePriceCents", "6000000"))
                .andExpect(status().isOk())
                .andReturn();

        List<ModelDto> models = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertThat(models).extracting("name").containsExactly("Terra");
    }

    @Test
    void getAllModelsCombinesBrandAndSpecFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/models")
                        .param("brand", "Aster")
                        .param("minPowerPs", "500")
                        .param("maxTopSpeedKph", "310"))
                .andExpect(status().isOk())
                .andReturn();

        List<ModelDto> models = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertThat(models).extracting("name").containsExactly("Vale");
        assertThat(models.getFirst().trims()).isNotEmpty();
        assertThat(models.getFirst().options()).isNotEmpty();
    }

    @Test
    void getAllModelsRejectsInvertedRanges() throws Exception {
        mockMvc.perform(get("/api/models")
                        .param("minPowerPs", "600")
                        .param("maxPowerPs", "100"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getModelByIdReturnsNestedTrimsAndOptions() throws Exception {
        mockMvc.perform(get("/api/models/{id}", VALE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VALE_ID.toString()))
                .andExpect(jsonPath("$.brand").value("Aster"))
                .andExpect(jsonPath("$.name").value("Vale"))
                .andExpect(jsonPath("$.modelYear").value(2026))
                .andExpect(jsonPath("$.basePriceCents").value(4_590_000))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.engine").value("Twin-turbo V6"))
                .andExpect(jsonPath("$.powerPs").value(520))
                .andExpect(jsonPath("$.accelerationS").value(3.9))
                .andExpect(jsonPath("$.topSpeedKph").value(300))
                .andExpect(jsonPath("$.drivetrain").value("AWD"))
                .andExpect(jsonPath("$.seats").value(4))
                .andExpect(jsonPath("$.trims[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Touring", "Apex")))
                .andExpect(jsonPath("$.trims[?(@.name == 'Apex')].priceDeltaCents").value(610_000))
                .andExpect(jsonPath("$.options[*].name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Panoramic canopy", "Velocity package", "Comfort seats", "Infotainment pro")))
                .andExpect(jsonPath("$.options[?(@.name == 'Panoramic canopy')].priceCents").value(145_000));
    }

    @Test
    void getModelByIdReturns404WithErrorBodyForUnknownId() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        mockMvc.perform(get("/api/models/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MODEL_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getModelByIdReturns422ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/models/not-a-uuid"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
