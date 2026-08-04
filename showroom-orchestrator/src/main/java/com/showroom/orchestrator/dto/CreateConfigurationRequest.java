package com.showroom.orchestrator.dto;

import java.util.List;

/**
 * Payload forwarded to the downstream Vehicle Config service
 * ({@code POST /api/configurations}).
 */
public record CreateConfigurationRequest(
        String modelId,
        String trimId,
        List<String> optionIds) {
}
