package com.showroom.vehicle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showroom.vehicle.dto.ConfigurationDto;
import com.showroom.vehicle.domain.ConfigurationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfigurationApiIntegrationTest {

    private static final UUID VALE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TERRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TOURING_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID APEX_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID PIONEER_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID PANORAMIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID VELOCITY_VALE_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID VELOCITY_TERRA_ID = UUID.fromString("00000000-0000-0000-0000-000000000025");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createConfigurationReturns201WithServerCalculatedTotal() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of(PANORAMIC_ID.toString(), VELOCITY_VALE_ID.toString())));

        MvcResult result = mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalPriceCents").value(5_000_000))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.model.id").value(VALE_ID.toString()))
                .andExpect(jsonPath("$.model.name").value("Vale"))
                .andExpect(jsonPath("$.trim.id").value(TOURING_ID.toString()))
                .andExpect(jsonPath("$.trim.name").value("Touring"))
                .andExpect(jsonPath("$.options[*].name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Panoramic canopy", "Velocity package")))
                .andReturn();

        ConfigurationDto dto = objectMapper.readValue(
                result.getResponse().getContentAsString(), ConfigurationDto.class);
        assertThat(dto.id()).isNotNull();
        assertThat(dto.status()).isEqualTo(ConfigurationStatus.DRAFT);
        assertThat(dto.options()).hasSize(2);

        mockMvc.perform(get("/api/configurations/{id}", dto.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.id().toString()))
                .andExpect(jsonPath("$.totalPriceCents").value(5_000_000))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.model.id").value(VALE_ID.toString()))
                .andExpect(jsonPath("$.trim.name").value("Touring"))
                .andExpect(jsonPath("$.options.length()").value(2));
    }

    @Test
    void createConfigurationAllowsEmptyOptionIds() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of()));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPriceCents").value(4_590_000))
                .andExpect(jsonPath("$.options.length()").value(0));
    }

    @Test
    void createConfigurationRejectsTrimNotForModel() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", PIONEER_ID.toString(),
                "optionIds", List.of()));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("TRIM_NOT_FOR_MODEL"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createConfigurationRejectsOptionNotForModel() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of(VELOCITY_TERRA_ID.toString())));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("OPTION_NOT_FOR_MODEL"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createConfigurationRejectsDuplicateOptionIds() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of(PANORAMIC_ID.toString(), PANORAMIC_ID.toString())));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_OPTION_IDS"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createConfigurationReturns404ForUnknownModel() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", UNKNOWN_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of()));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MODEL_NOT_FOUND"));
    }

    @Test
    void createConfigurationReturns404ForUnknownTrim() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", UNKNOWN_ID.toString(),
                "optionIds", List.of()));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRIM_NOT_FOUND"));
    }

    @Test
    void createConfigurationReturns404ForUnknownOption() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "modelId", VALE_ID.toString(),
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of(UNKNOWN_ID.toString())));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("OPTION_NOT_FOUND"));
    }

    @Test
    void createConfigurationReturns422ForMissingModelId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "trimId", TOURING_ID.toString(),
                "optionIds", List.of()));

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void createConfigurationReturns422ForMalformedBody() throws Exception {
        String body = "{\"modelId\":\"not-a-uuid\",\"trimId\":\"" + TOURING_ID + "\"}";

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createConfigurationReturns422ForNullOptionId() throws Exception {
        String body = "{\"modelId\":\"" + VALE_ID + "\",\"trimId\":\""
                + TOURING_ID + "\",\"optionIds\":[null]}";

        mockMvc.perform(post("/api/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getConfigurationReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/configurations/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CONFIGURATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
