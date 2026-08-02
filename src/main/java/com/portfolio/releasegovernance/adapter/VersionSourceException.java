package com.portfolio.releasegovernance.adapter;

import java.time.Instant;

public class VersionSourceException extends RuntimeException {
    private final boolean rateLimited;
    private final Instant retryAfter;

    public VersionSourceException(String message, Throwable cause) {
        this(message, cause, false, null);
    }

    public VersionSourceException(String message, boolean rateLimited, Instant retryAfter) {
        this(message, null, rateLimited, retryAfter);
    }

    private VersionSourceException(String message, Throwable cause, boolean rateLimited, Instant retryAfter) {
        super(message, cause);
        this.rateLimited = rateLimited;
        this.retryAfter = retryAfter;
    }

    public boolean isRateLimited() { return rateLimited; }
    public Instant getRetryAfter() { return retryAfter; }
}
