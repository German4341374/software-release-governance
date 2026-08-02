package com.portfolio.releasegovernance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GovernanceApiIntegrationTest extends AbstractPostgresIntegrationTest {

    @LocalServerPort private int port;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    void healthAndDashboardExposeSeededState() throws Exception {
        HttpResponse<String> health = get("/health");
        HttpResponse<String> dashboard = get("/api/dashboard");

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");
        assertThat(dashboard.statusCode()).isEqualTo(200);
        assertThat(dashboard.body()).contains("\"products\":1", "\"environment\":\"Production\"");
    }

    @Test
    void manualReleaseImportIsIdempotent() throws Exception {
        String externalId = "integration:" + UUID.randomUUID();
        String body = """
                {"version":"3.1.0","channel":"STABLE","externalId":"%s","notes":"Integration test"}
                """.formatted(externalId);

        HttpResponse<String> first = post("/api/products/10000000-0000-0000-0000-000000000001/releases", body);
        HttpResponse<String> second = post("/api/products/10000000-0000-0000-0000-000000000001/releases", body);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.body()).contains("\"imported\":1", "\"skipped\":0");
        assertThat(second.statusCode()).isEqualTo(201);
        assertThat(second.body()).contains("\"imported\":0", "\"skipped\":1");
    }

    @Test
    void invalidInputUsesProblemDetails() throws Exception {
        HttpResponse<String> response = post("/api/products", """
                {"name":"","vendor":"","sourceType":"MANUAL","defaultChannel":"STABLE"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("content-type").orElse(""))
                .contains("application/problem+json");
        assertThat(response.body()).contains("VALIDATION_FAILED", "One or more request fields are invalid.");
    }

    @Test
    void homePageRendersGovernanceDashboard() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Software Release Governance", "Operations Portal", "Outdated environments");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Actor", "integration-test")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
