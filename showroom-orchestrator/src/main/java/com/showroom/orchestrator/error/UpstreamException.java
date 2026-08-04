package com.showroom.orchestrator.error;

/**
 * A failure from the downstream Vehicle Config service. Carries the GraphQL
 * extensions code that should be surfaced to the client.
 */
public class UpstreamException extends RuntimeException {

    private final String code;
    private final int statusCode;

    public UpstreamException(String code, int statusCode, String message) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public UpstreamException(String code, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
