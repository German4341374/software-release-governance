package com.portfolio.releasegovernance.adapter;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.sources")
public record VersionSourceProperties(
        Duration connectTimeout,
        Duration requestTimeout,
        int maxAttempts,
        Duration initialBackoff,
        URI githubApiBase,
        boolean allowHttp) {

    public VersionSourceProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(8) : requestTimeout;
        maxAttempts = Math.clamp(maxAttempts, 1, 5);
        initialBackoff = initialBackoff == null ? Duration.ofMillis(250) : initialBackoff;
        githubApiBase = githubApiBase == null ? URI.create("https://api.github.com") : githubApiBase;
    }
}
