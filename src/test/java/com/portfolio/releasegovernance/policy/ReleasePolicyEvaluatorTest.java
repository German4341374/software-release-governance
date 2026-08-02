package com.portfolio.releasegovernance.policy;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.releasegovernance.domain.AvailableRelease;
import com.portfolio.releasegovernance.domain.Product;
import com.portfolio.releasegovernance.domain.ReleaseEnvironment;
import com.portfolio.releasegovernance.domain.ReleasePolicy;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReleasePolicyEvaluatorTest {
    private static final Instant TUESDAY_AT_02_UTC = Instant.parse("2026-08-04T02:00:00Z");

    private final ReleasePolicyEvaluator evaluator = new ReleasePolicyEvaluator();
    private Product product;
    private ReleaseEnvironment production;
    private ReleasePolicy policy;

    @BeforeEach
    void setUp() {
        product = new Product("Operations Portal", "Example Systems", "Test product",
                SourceType.MANUAL, null, ReleaseChannel.STABLE);
        production = new ReleaseEnvironment("Production", EnvironmentType.PRODUCTION, "UTC",
                LocalTime.of(1, 0), LocalTime.of(5, 0), "TUESDAY,THURSDAY");
        policy = new ReleasePolicy(product);
        policy.update(true, true, "1.2.0", "1.3.4,2.0.*", true, true,
                "test", TUESDAY_AT_02_UTC);
    }

    @Test
    void permitsApprovedStableReleaseInsideWindow() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.4.0", false),
                "1.2.0", true, false, TUESDAY_AT_02_UTC);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.violations()).isEmpty();
    }

    @Test
    void requiresApprovalForProduction() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.4.0", false),
                "1.2.0", false, false, TUESDAY_AT_02_UTC);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.violations()).contains("PRODUCTION_APPROVAL_REQUIRED");
    }

    @Test
    void blocksPrereleaseInProduction() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.5.0-beta.1", true),
                "1.2.0", true, false, TUESDAY_AT_02_UTC);

        assertThat(decision.violations()).contains("PRERELEASE_FORBIDDEN_IN_PRODUCTION");
    }

    @Test
    void blocksExactAndWildcardVersions() {
        assertThat(evaluator.evaluate(policy, production, release("1.3.4", false),
                "1.2.0", true, false, TUESDAY_AT_02_UTC).violations()).contains("VERSION_BLOCKED");
        assertThat(evaluator.evaluate(policy, production, release("2.0.7", false),
                "1.2.0", true, false, TUESDAY_AT_02_UTC).violations()).contains("VERSION_BLOCKED");
    }

    @Test
    void blocksReleaseBelowMinimumSupportedVersion() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.1.9", false),
                "1.0.0", true, false, TUESDAY_AT_02_UTC);

        assertThat(decision.violations()).contains("BELOW_MINIMUM_SUPPORTED_VERSION");
    }

    @Test
    void enforcesMaintenanceWindow() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.4.0", false),
                "1.2.0", true, false, Instant.parse("2026-08-05T12:00:00Z"));

        assertThat(decision.violations()).contains("OUTSIDE_MAINTENANCE_WINDOW");
    }

    @Test
    void recordsExplicitEmergencyBypasses() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.4.0", false),
                "1.2.0", false, true, Instant.parse("2026-08-05T12:00:00Z"));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.bypasses())
                .containsExactlyInAnyOrder("PRODUCTION_APPROVAL_BYPASSED", "MAINTENANCE_WINDOW_BYPASSED");
    }

    @Test
    void requiresEmergencyForDowngrade() {
        PolicyDecision decision = evaluator.evaluate(policy, production, release("1.2.0", false),
                "1.4.0", true, false, TUESDAY_AT_02_UTC);

        assertThat(decision.violations()).contains("DOWNGRADE_REQUIRES_EMERGENCY");
    }

    private AvailableRelease release(String version, boolean prerelease) {
        return new AvailableRelease(product, version,
                prerelease ? ReleaseChannel.BETA : ReleaseChannel.STABLE, prerelease,
                SourceType.MANUAL, "test:" + version, null, null, TUESDAY_AT_02_UTC);
    }
}
