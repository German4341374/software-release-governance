package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import tools.jackson.databind.JsonNode;
import com.portfolio.releasegovernance.domain.Product;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubReleasesAdapter implements VersionSourceAdapter {
    private final ResilientJsonClient client;
    private final VersionSourceProperties properties;

    public GitHubReleasesAdapter(ResilientJsonClient client, VersionSourceProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override public SourceType sourceType() { return SourceType.GITHUB_RELEASES; }

    @Override
    public List<ReleaseCandidate> fetch(Product product) {
        String repository = Objects.requireNonNullElse(product.getSourceReference(), "").trim();
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new VersionSourceException("GitHub sourceReference must use owner/repository.", null);
        }
        URI uri = UriComponentsBuilder.fromUri(properties.githubApiBase())
                .pathSegment("repos")
                .pathSegment(repository.split("/"))
                .pathSegment("releases")
                .queryParam("per_page", 50)
                .build().toUri();
        Map<String, String> headers = new HashMap<>();
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) headers.put("Authorization", "Bearer " + token);
        JsonNode body = client.get(uri, headers);
        if (!body.isArray()) throw new VersionSourceException("GitHub Releases response must be an array.", null);
        List<ReleaseCandidate> candidates = new ArrayList<>();
        for (JsonNode item : body) {
            if (item.path("draft").asBoolean(false)) continue;
            String tag = item.path("tag_name").asText();
            boolean prerelease = item.path("prerelease").asBoolean(false);
            try {
                candidates.add(new ReleaseCandidate(
                        tag,
                        prerelease ? ReleaseChannel.BETA : ReleaseChannel.STABLE,
                        prerelease,
                        sourceType(),
                        item.path("id").asText(tag),
                        item.path("html_url").asText(null),
                        item.path("body").asText(null),
                        parseInstant(item.path("published_at").asText(null))));
            } catch (IllegalArgumentException ignored) {
                // Non-semantic tags are outside this service's supported contract.
            }
        }
        return candidates;
    }

    private Instant parseInstant(String value) { return value == null || value.isBlank() ? null : Instant.parse(value); }
}
