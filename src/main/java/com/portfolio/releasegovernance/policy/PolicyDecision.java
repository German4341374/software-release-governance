package com.portfolio.releasegovernance.policy;

import java.util.List;

public record PolicyDecision(boolean allowed, List<String> violations, List<String> warnings, List<String> bypasses) {
    public static PolicyDecision of(List<String> violations, List<String> warnings, List<String> bypasses) {
        return new PolicyDecision(violations.isEmpty(), List.copyOf(violations), List.copyOf(warnings), List.copyOf(bypasses));
    }
}
