package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.releasegovernance.domain.Product;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VersionSourceAdapterTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void staticJsonAdapterNormalizesReleaseCandidates() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/releases", exchange -> {
            byte[] response = """
                    {"releases":[{"id":"build-204","version":"v2.4.0","channel":"STABLE",
                    "prerelease":false,"publishedAt":"2026-08-01T10:00:00Z","notes":"Security update"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
        StaticJsonAdapter adapter = new StaticJsonAdapter(client(true, 1));
        Product product = new Product("Portal", "Example", null, SourceType.STATIC_JSON,
                endpoint.toString(), ReleaseChannel.STABLE);

        List<ReleaseCandidate> candidates = adapter.fetch(product);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().version()).isEqualTo("2.4.0");
        assertThat(candidates.getFirst().externalId()).isEqualTo("build-204");
    }

    @Test
    void transientGatewayFailureIsRetried() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/retry", exchange -> {
            int status = requests.incrementAndGet() == 1 ? 503 : 200;
            byte[] response = (status == 200 ? "{\"ok\":true}" : "temporary")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/retry");

        assertThat(client(true, 2).get(endpoint, java.util.Map.of()).path("ok").asBoolean()).isTrue();
        assertThat(requests).hasValue(2);
    }

    @Test
    void rejectsPlainHttpInProductionMode() {
        assertThatThrownBy(() -> client(false, 1).get(URI.create("http://example.com/releases"), java.util.Map.of()))
                .isInstanceOf(VersionSourceException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsCredentialsEmbeddedInUrl() {
        assertThatThrownBy(() -> client(true, 1).get(URI.create("https://user:secret@example.com/releases"), java.util.Map.of()))
                .isInstanceOf(VersionSourceException.class)
                .hasMessageContaining("invalid");
    }

    private ResilientJsonClient client(boolean allowHttp, int attempts) {
        VersionSourceProperties properties = new VersionSourceProperties(
                Duration.ofSeconds(1), Duration.ofSeconds(2), attempts,
                Duration.ofMillis(1), URI.create("https://api.github.com"), allowHttp);
        return new ResilientJsonClient(properties, new ObjectMapper());
    }
}
