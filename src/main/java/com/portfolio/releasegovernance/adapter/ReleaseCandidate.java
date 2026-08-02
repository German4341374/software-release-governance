package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.policy.SemanticVersion;
import java.time.Instant;

public record ReleaseCandidate(
        String version,
        ReleaseChannel channel,
        boolean prerelease,
        SourceType sourceType,
        String externalId,
        String releaseNotesUrl,
        String notes,
        Instant publishedAt) {

    public ReleaseCandidate {
        version = SemanticVersion.parse(version).canonical();
        prerelease = prerelease || SemanticVersion.parse(version).isPrerelease();
        notes = notes == null ? null : notes.substring(0, Math.min(notes.length(), 4000));
    }
}
