package com.showroom.orchestrator.graphql;

import com.showroom.orchestrator.error.InvalidInputException;
import com.showroom.orchestrator.error.UpstreamException;
import com.showroom.orchestrator.error.UpstreamTimeoutException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/**
 * Translates exceptions raised by data fetchers (including errors surfaced
 * from the blocking downstream {@code RestClient}) into GraphQL errors with a
 * stable {@code code} extension, e.g. NOT_FOUND, BAD_REQUEST, UPSTREAM_ERROR,
 * TIMEOUT, VALIDATION_ERROR or INTERNAL_ERROR.
 *
 * <p>This servlet transport uses the synchronous adapter hook
 * {@link #resolveToSingleError} rather than the Reactor-based interface method.
 */
@Component
public class ShowroomExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(ShowroomExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        Throwable cause = unwrap(exception);

        String code;
        String message;
        if (cause instanceof UpstreamException upstream) {
            code = upstream.getCode();
            message = upstream.getStatusCode() >= 500
                    ? "Vehicle service is unavailable"
                    : upstream.getMessage();
        } else if (cause instanceof UpstreamTimeoutException || cause instanceof TimeoutException) {
            code = "TIMEOUT";
            message = cause.getMessage() != null ? cause.getMessage() : "Request to downstream timed out";
        } else if (cause instanceof InvalidInputException invalid) {
            code = "VALIDATION_ERROR";
            message = invalid.getMessage();
        } else if (cause instanceof DecodingException) {
            code = "UPSTREAM_ERROR";
            message = "Downstream returned an unreadable response";
        } else {
            code = "INTERNAL_ERROR";
            message = "The request could not be completed";
        }

        log.warn("GraphQL data fetcher error (code={}) for operation {}: {}",
                code,
                environment != null && environment.getOperationDefinition() != null
                        ? environment.getOperationDefinition().getName() : "?",
                message, cause);

        return GraphqlErrorBuilder.newError(environment)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
