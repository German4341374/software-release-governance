package com.portfolio.releasegovernance.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticVersionTest {

    @Test
    void acceptsOptionalVPrefixAndBuildMetadata() {
        assertThat(SemanticVersion.parse("v2.4.1-rc.2+build.77").canonical())
                .isEqualTo("2.4.1-rc.2+build.77");
    }

    @Test
    void followsOfficialPrereleasePrecedence() {
        List<String> ordered = List.of(
                "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
                "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0");

        for (int index = 0; index < ordered.size() - 1; index++) {
            assertThat(SemanticVersion.parse(ordered.get(index)))
                    .isLessThan(SemanticVersion.parse(ordered.get(index + 1)));
        }
    }

    @Test
    void ignoresBuildMetadataForPrecedence() {
        assertThat(SemanticVersion.parse("1.3.0+linux"))
                .isEqualByComparingTo(SemanticVersion.parse("1.3.0+windows"));
    }

    @Test
    void comparesMajorMinorAndPatchNumerically() {
        assertThat(SemanticVersion.parse("10.2.0")).isGreaterThan(SemanticVersion.parse("2.99.99"));
        assertThat(SemanticVersion.parse("2.10.0")).isGreaterThan(SemanticVersion.parse("2.9.99"));
        assertThat(SemanticVersion.parse("2.10.11")).isGreaterThan(SemanticVersion.parse("2.10.2"));
    }

    @Test
    void rejectsInvalidVersions() {
        for (String invalid : List.of("1", "1.2", "01.2.3", "1.2.3-01", "latest", "")) {
            assertThatThrownBy(() -> SemanticVersion.parse(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
