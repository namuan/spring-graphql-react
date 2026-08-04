package com.showroom.orchestrator.error;

public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
