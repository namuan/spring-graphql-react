package com.showroom.orchestrator;

import com.showroom.orchestrator.dto.Trim;
import com.showroom.orchestrator.dto.VehicleConfiguration;
import com.showroom.orchestrator.dto.VehicleModel;
import com.showroom.orchestrator.dto.VehicleOption;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GraphQlTester integration tests with the downstream Vehicle Config service
 * faked by an OkHttp MockWebServer. The orchestrator's RestClient is pointed at
 * the mock via {@code vehicle-service.url}, so the whole GraphQL ->
 * orchestrator -> HTTP boundary is exercised for real.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class ShowroomGraphQlControllerTest {

    private static final MockWebServer DOWNSTREAM = new MockWebServer();

    static {
        try {
            DOWNSTREAM.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start MockWebServer", e);
        }
    }

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("vehicle-service.url", () -> DOWNSTREAM.url("/").toString());
        // Short request timeout so the timeout test stays fast.
        registry.add("vehicle-service.request-timeout", () -> "500");
    }

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private JsonMapper objectMapper;

    @AfterAll
    static void shutdown() throws IOException {
        DOWNSTREAM.shutdown();
    }

    @Test
    void modelsQueryReturnsModelsFromDownstream() throws Exception {
        VehicleModel roadster = new VehicleModel(
                "m1", "Acme", "Roadster", 2025, 4_500_000L, "Two-seat roadster",
                "Twin-turbo V6", 520, new java.math.BigDecimal("3.9"), 300, "AWD", null, 4,
                List.of(), List.of());
        DOWNSTREAM.enqueue(jsonResponse(objectMapper.writeValueAsString(List.of(roadster))));

        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  models {
                    id
                    brand
                    name
                    modelYear
                    basePriceCents
                    description
                    engine
                    powerPs
                    accelerationS
                    topSpeedKph
                    drivetrain
                    rangeKm
                    seats
                    trims { id name priceDeltaCents }
                    options { id name category priceCents }
                  }
                }
                """).execute();

        response.path("models").entityList(VehicleModel.class).hasSize(1);
        response.path("models[0].id").entity(String.class).isEqualTo("m1");
        response.path("models[0].brand").entity(String.class).isEqualTo("Acme");
        response.path("models[0].modelYear").entity(Integer.class).isEqualTo(2025);
        response.path("models[0].basePriceCents").entity(Integer.class).isEqualTo(4_500_000);
        response.path("models[0].engine").entity(String.class).isEqualTo("Twin-turbo V6");
        response.path("models[0].powerPs").entity(Integer.class).isEqualTo(520);
        response.path("models[0].accelerationS").entity(Double.class).isEqualTo(3.9);
        response.path("models[0].topSpeedKph").entity(Integer.class).isEqualTo(300);
        response.path("models[0].drivetrain").entity(String.class).isEqualTo("AWD");
        response.path("models[0].seats").entity(Integer.class).isEqualTo(4);

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/models");
    }

    @Test
    void modelsQueryPassesFilterToDownstreamAsQueryParams() throws Exception {
        DOWNSTREAM.enqueue(jsonResponse("[]"));

        graphQlTester.document("""
                query($filter: ModelFilter) {
                  models(filter: $filter) { id }
                }
                """)
                .variable("filter", Map.of(
                        "brand", "Aster",
                        "minPowerPs", 500,
                        "minAccelerationS", 4.0,
                        "maxBasePriceCents", 6_000_000))
                .execute();

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo(
                "/api/models?brand=Aster&maxBasePriceCents=6000000&minPowerPs=500&minAccelerationS=4.0");
    }

    @Test
    void configurationQueryReturnsConfigurationFromDownstream() throws Exception {
        DOWNSTREAM.enqueue(jsonResponse(objectMapper.writeValueAsString(exampleConfiguration())));

        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  configuration(id: "c1") {
                    id
                    status
                    totalPriceCents
                    createdAt
                    model { id brand }
                    trim { id name }
                  }
                }
                """).execute();

        response.path("configuration.id").entity(String.class).isEqualTo("c1");
        response.path("configuration.status").entity(String.class).isEqualTo("CREATED");
        response.path("configuration.trim.name").entity(String.class).isEqualTo("GT");

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/configurations/c1");
    }

    @Test
    void createConfigurationMutationPostsToDownstream() throws Exception {
        DOWNSTREAM.enqueue(jsonResponse(objectMapper.writeValueAsString(exampleConfiguration())));

        GraphQlTester.Response response = graphQlTester.document("""
                mutation Create($input: CreateConfigurationInput!) {
                  createConfiguration(input: $input) {
                    id
                    model { id brand name }
                    trim { id name priceDeltaCents }
                    options { id name category priceCents }
                    totalPriceCents
                    status
                    createdAt
                  }
                }
                """)
                .variable("input", Map.of("modelId", "m1", "trimId", "t1", "optionIds", List.of("o1")))
                .execute();

        response.path("createConfiguration.id").entity(String.class).isEqualTo("c1");
        response.path("createConfiguration.model.id").entity(String.class).isEqualTo("m1");
        response.path("createConfiguration.trim.name").entity(String.class).isEqualTo("GT");
        response.path("createConfiguration.totalPriceCents").entity(Integer.class).isEqualTo(4_740_000);
        response.path("createConfiguration.status").entity(String.class).isEqualTo("CREATED");

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/configurations");
        Map<String, Object> body = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {
        });
        assertThat(body).containsEntry("modelId", "m1").containsEntry("trimId", "t1");
        assertThat(body.get("optionIds")).isEqualTo(List.of("o1"));
    }

    @Test
    void modelQueryMapsDownstreamNotFoundToGraphQlError() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errorCode\":\"NOT_FOUND\",\"message\":\"Model not found\"}"));

        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  model(id: "missing") {
                    id
                    brand
                  }
                }
                """).execute();

        response.errors().satisfy(errors -> {
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getExtensions()).containsEntry("code", "NOT_FOUND");
        });

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/models/missing");
    }

    @Test
    void createConfigurationMapsDownstreamServerErrorToGraphQlError() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        GraphQlTester.Response response = graphQlTester.document("""
                mutation Create($input: CreateConfigurationInput!) {
                  createConfiguration(input: $input) { id }
                }
                """)
                .variable("input", Map.of("modelId", "m1", "trimId", "t1", "optionIds", List.<String>of()))
                .execute();

        response.errors().satisfy(errors -> {
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0).getExtensions()).containsEntry("code", "UPSTREAM_ERROR");
        });

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/configurations");
    }

    @Test
    void createConfigurationValidatesRequiredInput() {
        GraphQlTester.Response response = graphQlTester.document("""
                mutation Create($input: CreateConfigurationInput!) {
                  createConfiguration(input: $input) { id }
                }
                """)
                .variable("input", Map.of("trimId", "t1", "optionIds", List.<String>of()))
                .execute();

        response.errors().satisfy(errors -> {
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0).getExtensions()).containsEntry("code", "VALIDATION_ERROR");
        });
    }

    @Test
    void upstreamTimeoutMapsToGraphQlError() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setBodyDelay(3, TimeUnit.SECONDS).setBody("{}"));

        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  models { id }
                }
                """).execute();

        response.errors().satisfy(errors -> {
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0).getExtensions()).containsEntry("code", "TIMEOUT");
        });

        RecordedRequest request = DOWNSTREAM.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/models");
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static VehicleConfiguration exampleConfiguration() {
        VehicleModel model = new VehicleModel(
                "m1", "Acme", "Roadster", 2025, 4_500_000L, "Two-seat roadster",
                "Twin-turbo V6", 520, new java.math.BigDecimal("3.9"), 300, "AWD", null, 4,
                List.of(), List.of());
        Trim gt = new Trim("t1", "GT", 150_000L);
        VehicleOption leather = new VehicleOption("o1", "Leather", "Interior", 90_000L);
        return new VehicleConfiguration(
                "c1", "CREATED", "2026-08-04T10:00:00Z", 4_740_000L, model, gt, List.of(leather));
    }
}
