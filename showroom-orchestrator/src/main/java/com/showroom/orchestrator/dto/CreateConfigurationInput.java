package com.showroom.orchestrator.dto;

import java.util.List;

/**
 * GraphQL mutation input. Fields are intentionally nullable in the schema so
 * that the orchestrator can validate them and return a GraphQL error with a
 * {@code VALIDATION_ERROR} extensions code instead of a raw coercion error.
 */
public record CreateConfigurationInput(
        String modelId,
        String trimId,
        List<String> optionIds) {
}
