package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.AuditAction;

import com.portfolio.releasegovernance.domain.*;
import com.portfolio.releasegovernance.policy.SemanticVersion;
import com.portfolio.releasegovernance.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleasePolicyService {
    private final ReleasePolicyRepository policies;
    private final ProductRepository products;
    private final AuditService audit;
    private final Clock clock;

    public ReleasePolicyService(ReleasePolicyRepository policies, ProductRepository products,
                                AuditService audit, Clock clock) {
        this.policies = policies;
        this.products = products;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PolicyResponse get(UUID productId) { return toResponse(find(productId)); }

    @Transactional
    public PolicyResponse update(UUID productId, UpdatePolicyRequest request, String correlationId) {
        validateVersions(request.minimumSupportedVersion(), request.blockedVersions());
        ReleasePolicy policy = find(productId);
        if (policy.getLockVersion() != request.expectedVersion()) {
            throw new GovernanceRuleException("CONCURRENCY_CONFLICT", "Release policy changed after it was read.");
        }
        policy.update(request.prohibitPrereleaseInProduction(), request.requireProductionApproval(),
                trim(request.minimumSupportedVersion()), trim(request.blockedVersions()),
                request.enforceMaintenanceWindow(), request.emergencyBypassAllowed(),
                request.actor().trim(), clock.instant());
        audit.record(AuditAction.POLICY_CHANGED, "ReleasePolicy", policy.getId(), request.actor(),
                Map.of("productId", productId, "minimumSupportedVersion", Objects.toString(request.minimumSupportedVersion(), ""),
                        "blockedVersions", Objects.toString(request.blockedVersions(), "")), correlationId);
        return toResponse(policy);
    }

    ReleasePolicy find(UUID productId) {
        return policies.findByProductId(productId).orElseGet(() -> {
            Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
            return policies.save(new ReleasePolicy(product));
        });
    }

    private void validateVersions(String minimum, String blocked) {
        if (minimum != null && !minimum.isBlank()) SemanticVersion.parse(minimum);
        if (blocked == null || blocked.isBlank()) return;
        for (String pattern : blocked.split(",")) {
            String value = pattern.trim();
            if (value.isBlank()) continue;
            if (value.endsWith(".*")) SemanticVersion.parse(value.substring(0, value.length() - 2) + ".0");
            else SemanticVersion.parse(value);
        }
    }

    static PolicyResponse toResponse(ReleasePolicy value) {
        return new PolicyResponse(value.getId(), value.getProduct().getId(),
                value.isProhibitPrereleaseInProduction(), value.isRequireProductionApproval(),
                value.getMinimumSupportedVersion(), value.getBlockedVersions(),
                value.isEnforceMaintenanceWindow(), value.isEmergencyBypassAllowed(),
                value.getUpdatedAt(), value.getUpdatedBy(), value.getLockVersion());
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
}
