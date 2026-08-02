package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.AvailableRelease;
import com.portfolio.releasegovernance.policy.SemanticVersion;
import com.portfolio.releasegovernance.repository.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ProductRepository products;
    private final EnvironmentRepository environments;
    private final AvailableReleaseRepository releases;
    private final InstalledVersionRepository installed;
    private final ApprovalRepository approvals;
    private final DeploymentRecordRepository deployments;

    public DashboardService(ProductRepository products, EnvironmentRepository environments,
                            AvailableReleaseRepository releases, InstalledVersionRepository installed,
                            ApprovalRepository approvals, DeploymentRecordRepository deployments) {
        this.products = products;
        this.environments = environments;
        this.releases = releases;
        this.installed = installed;
        this.approvals = approvals;
        this.deployments = deployments;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<UUID, AvailableRelease> latest = releases.findAllByOrderByImportedAtDesc().stream()
                .filter(value -> value.getChannel() == ReleaseChannel.STABLE && !value.isPrerelease()
                        && value.getStatus() != ReleaseStatus.BLOCKED)
                .collect(Collectors.toMap(value -> value.getProduct().getId(), Function.identity(),
                        (left, right) -> SemanticVersion.parse(left.getVersion()).compareTo(SemanticVersion.parse(right.getVersion())) >= 0
                                ? left : right));
        List<OutdatedEnvironment> rows = installed.findAllByOrderByProductNameAscEnvironmentNameAsc().stream()
                .map(value -> {
                    AvailableRelease newest = latest.get(value.getProduct().getId());
                    String latestVersion = newest == null ? null : newest.getVersion();
                    boolean outdated = latestVersion != null && SemanticVersion.parse(value.getVersion())
                            .compareTo(SemanticVersion.parse(latestVersion)) < 0;
                    return new OutdatedEnvironment(value.getProduct().getId(), value.getProduct().getName(),
                            value.getEnvironment().getId(), value.getEnvironment().getName(),
                            value.getVersion(), latestVersion, outdated);
                }).toList();
        return new DashboardResponse(products.count(), environments.count(), releases.count(),
                approvals.countByStatus(ApprovalStatus.PENDING), deployments.countByStatus(DeploymentStatus.FAILED), rows);
    }
}
