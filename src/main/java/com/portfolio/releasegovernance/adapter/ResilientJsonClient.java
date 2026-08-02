package com.portfolio.releasegovernance.adapter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ResilientJsonClient {
    private final VersionSourceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public ResilientJsonClient(VersionSourceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public JsonNode get(URI uri, Map<String, String> headers) {
        validate(uri);
        VersionSourceException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                        .timeout(properties.requestTimeout())
                        .header("Accept", "application/json")
                        .header("User-Agent", "software-release-governance/1.0")
                        .GET();
                headers.forEach(request::header);
                HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) return objectMapper.readTree(response.body());
                if (status == 429 || (status == 403 && "0".equals(response.headers().firstValue("X-RateLimit-Remaining").orElse("")))) {
                    Instant retryAfter = retryAfter(response);
                    throw new VersionSourceException("Version source rate limit was reached.", true, retryAfter);
                }
                if (status == 502 || status == 503 || status == 504) {
                    lastFailure = new VersionSourceException("Temporary version source response: HTTP " + status, null);
                    if (attempt < properties.maxAttempts()) {
                        backoff(attempt);
                        continue;
                    }
                }
                throw new VersionSourceException("Version source returned HTTP " + status + ".", null);
            } catch (HttpTimeoutException exception) {
                lastFailure = new VersionSourceException("Version source request timed out.", exception);
            } catch (IOException exception) {
                lastFailure = new VersionSourceException("Version source request failed.", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new VersionSourceException("Version source request was interrupted.", exception);
            }
            if (attempt < properties.maxAttempts()) backoff(attempt);
        }
        throw lastFailure == null ? new VersionSourceException("Version source request failed.", null) : lastFailure;
    }

    private void validate(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new VersionSourceException("Version source URL is invalid.", null);
        }
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean developmentHttp = properties.allowHttp() && "http".equalsIgnoreCase(uri.getScheme());
        if (!https && !developmentHttp) throw new VersionSourceException("Version source URL must use HTTPS.", null);
        if (!properties.allowHttp()) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                            || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                        throw new VersionSourceException("Version source URL resolves to a non-public address.", null);
                    }
                }
            } catch (UnknownHostException exception) {
                throw new VersionSourceException("Version source host cannot be resolved.", exception);
            }
        }
    }

    private Instant retryAfter(HttpResponse<?> response) {
        String reset = response.headers().firstValue("X-RateLimit-Reset").orElse(null);
        if (reset != null) {
            try { return Instant.ofEpochSecond(Long.parseLong(reset)); }
            catch (NumberFormatException ignored) { /* Try Retry-After. */ }
        }
        String value = response.headers().firstValue("Retry-After").orElse("60");
        try { return Instant.now().plusSeconds(Math.clamp(Long.parseLong(value), 1, 86400)); }
        catch (NumberFormatException ignored) {
            try { return ZonedDateTime.parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
            catch (DateTimeParseException ignoredAgain) { return Instant.now().plusSeconds(60); }
        }
    }

    private void backoff(int attempt) {
        long base = Math.max(1, properties.initialBackoff().toMillis());
        long exponential = Math.min(base * (1L << (attempt - 1)), 5000);
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, exponential / 4));
        try { Thread.sleep(exponential + jitter); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new VersionSourceException("Retry delay was interrupted.", exception);
        }
    }
}
