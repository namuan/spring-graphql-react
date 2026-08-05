package com.showroom.orchestrator.client;

import tools.jackson.databind.json.JsonMapper;
import com.showroom.orchestrator.dto.CreateConfigurationRequest;
import com.showroom.orchestrator.dto.ModelFilter;
import com.showroom.orchestrator.dto.VehicleConfiguration;
import com.showroom.orchestrator.dto.VehicleModel;
import com.showroom.orchestrator.error.UpstreamException;
import com.showroom.orchestrator.error.UpstreamTimeoutException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Thin synchronous HTTP client for the downstream Vehicle Config service.
 *
 * <p>Every call maps HTTP errors to an {@link UpstreamException} carrying a
 * GraphQL extensions code. Codes are taken from the downstream error body
 * ({@code {"errorCode": "...", "message": "..."}}) when present, otherwise
 * derived from the HTTP status. Transport failures (connection refused,
 * read timeout) are translated into typed exceptions so the GraphQL layer can
 * surface them with a stable code.
 */
@Component
public class VehicleServiceClient {

    private static final ParameterizedTypeReference<List<VehicleModel>> MODEL_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final int MAX_BODY_SNIPPET = 300;

    private final RestClient restClient;
    private final JsonMapper objectMapper;

    public VehicleServiceClient(@Qualifier("vehicleServiceRestClient") RestClient restClient,
                                JsonMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<VehicleModel> getModels() {
        return getModels(null);
    }

    public List<VehicleModel> getModels(ModelFilter filter) {
        return guarded(() -> restClient.get()
                .uri(builder -> {
                    UriBuilder uri = builder.path("/api/models");
                    if (filter != null) {
                        if (filter.brand() != null) uri.queryParam("brand", filter.brand());
                        if (filter.minBasePriceCents() != null) uri.queryParam("minBasePriceCents", filter.minBasePriceCents());
                        if (filter.maxBasePriceCents() != null) uri.queryParam("maxBasePriceCents", filter.maxBasePriceCents());
                        if (filter.minPowerPs() != null) uri.queryParam("minPowerPs", filter.minPowerPs());
                        if (filter.maxPowerPs() != null) uri.queryParam("maxPowerPs", filter.maxPowerPs());
                        if (filter.minTopSpeedKph() != null) uri.queryParam("minTopSpeedKph", filter.minTopSpeedKph());
                        if (filter.maxTopSpeedKph() != null) uri.queryParam("maxTopSpeedKph", filter.maxTopSpeedKph());
                        if (filter.minAccelerationS() != null) uri.queryParam("minAccelerationS", filter.minAccelerationS());
                        if (filter.maxAccelerationS() != null) uri.queryParam("maxAccelerationS", filter.maxAccelerationS());
                        if (filter.minSeats() != null) uri.queryParam("minSeats", filter.minSeats());
                        if (filter.maxSeats() != null) uri.queryParam("maxSeats", filter.maxSeats());
                    }
                    return uri.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raiseUpstreamError)
                .body(MODEL_LIST_TYPE));
    }

    public VehicleModel getModel(String id) {
        return guarded(() -> restClient.get()
                .uri("/api/models/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raiseUpstreamError)
                .body(VehicleModel.class));
    }

    public VehicleConfiguration getConfiguration(String id) {
        return guarded(() -> restClient.get()
                .uri("/api/configurations/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raiseUpstreamError)
                .body(VehicleConfiguration.class));
    }

    public VehicleConfiguration createConfiguration(CreateConfigurationRequest request) {
        return guarded(() -> restClient.post()
                .uri("/api/configurations")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raiseUpstreamError)
                .body(VehicleConfiguration.class));
    }

    private void raiseUpstreamError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        ErrorBody errorBody = parseErrorBody(body);
        String code = resolveCode(status, errorBody);
        String detail = status.is5xxServerError() ? null
                : (errorBody != null && notBlank(errorBody.message()))
                ? errorBody.message()
                : truncate(body, MAX_BODY_SNIPPET);
        String message = status.is5xxServerError()
                ? "Vehicle service request failed"
                : notBlank(detail)
                ? detail
                : "Downstream request failed with HTTP " + status.value();
        throw new UpstreamException(code, status.value(), message);
    }

    private <T> T guarded(CheckedCall<T> call) {
        try {
            return call.call();
        } catch (UpstreamException e) {
            // Thrown by raiseUpstreamError; propagate as-is.
            throw e;
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new UpstreamTimeoutException("Vehicle service request timed out", e);
            }
            throw new UpstreamException("UPSTREAM_ERROR", 503, "Vehicle service is unavailable", e);
        } catch (Exception e) {
            if (isTimeout(e)) {
                throw new UpstreamTimeoutException("Vehicle service request timed out", e);
            }
            throw new UpstreamException("UPSTREAM_ERROR", 503, "Vehicle service is unavailable", e);
        }
    }

    /**
     * A read/connect timeout can surface as {@link SocketTimeoutException}
     * directly, or wrapped by the request factory / message converter in a
     * {@link RestClientException}. Walk the cause chain to catch both.
     */
    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record ErrorBody(String errorCode, String message) {
    }

    private ErrorBody parseErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, ErrorBody.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveCode(HttpStatusCode status, ErrorBody errorBody) {
        if (errorBody != null && notBlank(errorBody.errorCode())) {
            // Do not pass a generic 500 downstream as the orchestrator's own INTERNAL_ERROR.
            return "INTERNAL_ERROR".equals(errorBody.errorCode()) ? "UPSTREAM_ERROR" : errorBody.errorCode();
        }
        if (status.value() == 404) {
            return "NOT_FOUND";
        }
        if (status.is4xxClientError()) {
            return "BAD_REQUEST";
        }
        return "UPSTREAM_ERROR";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    @FunctionalInterface
    private interface CheckedCall<T> {
        T call() throws Exception;
    }
}
