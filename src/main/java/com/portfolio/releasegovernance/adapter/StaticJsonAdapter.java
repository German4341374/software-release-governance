package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import tools.jackson.databind.JsonNode;
import com.portfolio.releasegovernance.domain.Product;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class StaticJsonAdapter implements VersionSourceAdapter {
    private final ResilientJsonClient client;

    public StaticJsonAdapter(ResilientJsonClient client) { this.client = client; }

    @Override public SourceType sourceType() { return SourceType.STATIC_JSON; }

    @Override
    public List<ReleaseCandidate> fetch(Product product) {
        JsonNode root = client.get(URI.create(Objects.requireNonNullElse(product.getSourceReference(), "")), Map.of());
        JsonNode releases = root.path("releases");
        if (!releases.isArray()) throw new VersionSourceException("Static JSON must contain a releases array.", null);
        List<ReleaseCandidate> result = new ArrayList<>();
        for (JsonNode item : releases) {
            String version = item.path("version").asText();
            ReleaseChannel channel = ReleaseChannel.valueOf(item.path("channel").asText("STABLE").toUpperCase(Locale.ROOT));
            result.add(new ReleaseCandidate(
                    version,
                    channel,
                    item.path("prerelease").asBoolean(false),
                    sourceType(),
                    item.path("id").asText(version + ":" + channel),
                    item.path("url").asText(null),
                    item.path("notes").asText(null),
                    parseInstant(item.path("publishedAt").asText(null))));
        }
        return result;
    }

    private Instant parseInstant(String value) { return value == null || value.isBlank() ? null : Instant.parse(value); }
}
