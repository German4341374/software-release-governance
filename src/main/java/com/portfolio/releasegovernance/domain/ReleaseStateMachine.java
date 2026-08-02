package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.ReleaseStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ReleaseStateMachine {
    private static final Map<ReleaseStatus, Set<ReleaseStatus>> ALLOWED = new EnumMap<>(ReleaseStatus.class);

    static {
        ALLOWED.put(ReleaseStatus.DISCOVERED, EnumSet.of(ReleaseStatus.AWAITING_APPROVAL, ReleaseStatus.APPROVED, ReleaseStatus.BLOCKED));
        ALLOWED.put(ReleaseStatus.AWAITING_APPROVAL, EnumSet.of(ReleaseStatus.APPROVED, ReleaseStatus.BLOCKED));
        ALLOWED.put(ReleaseStatus.APPROVED, EnumSet.of(ReleaseStatus.SCHEDULED, ReleaseStatus.BLOCKED));
        ALLOWED.put(ReleaseStatus.SCHEDULED, EnumSet.of(ReleaseStatus.DEPLOYED, ReleaseStatus.APPROVED, ReleaseStatus.BLOCKED));
        ALLOWED.put(ReleaseStatus.DEPLOYED, EnumSet.of(ReleaseStatus.SUPERSEDED));
        ALLOWED.put(ReleaseStatus.BLOCKED, EnumSet.of(ReleaseStatus.DISCOVERED));
        ALLOWED.put(ReleaseStatus.SUPERSEDED, EnumSet.noneOf(ReleaseStatus.class));
    }

    private ReleaseStateMachine() {}

    public static boolean isAllowed(ReleaseStatus current, ReleaseStatus next) {
        return current != next && ALLOWED.get(current).contains(next);
    }

    public static void ensureAllowed(ReleaseStatus current, ReleaseStatus next) {
        if (!isAllowed(current, next)) {
            throw new GovernanceRuleException("INVALID_RELEASE_TRANSITION",
                    "Release cannot transition from " + current + " to " + next + ".");
        }
    }
}
