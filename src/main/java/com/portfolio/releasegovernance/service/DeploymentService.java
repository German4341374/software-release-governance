package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.*;
import com.portfolio.releasegovernance.policy.*;
import com.portfolio.releasegovernance.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeploymentService {
    private static final Set<DeploymentStatus> ACTIVE = EnumSet.of(DeploymentStatus.SCHEDULED, DeploymentStatus.IN_PROGRESS);
    private final DeploymentRecordRepository deployments;
    private final AvailableReleaseRepository releases;
    private final EnvironmentRepository environments;
    private final InstalledVersionRepository installedVersions;
    private final ApprovalRepository approvals;
    private final ReleasePolicyService policyService;
    private final ReleasePolicyEvaluator evaluator;
    private final AuditService audit;
    private final Clock clock;

    public DeploymentService(DeploymentRecordRepository deployments, AvailableReleaseRepository releases,
                             EnvironmentRepository environments, InstalledVersionRepository installedVersions,
                             ApprovalRepository approvals, ReleasePolicyService policyService,
                             ReleasePolicyEvaluator evaluator, AuditService audit, Clock clock) {
        this.deployments = deployments;
        this.releases = releases;
        this.environments = environments;
        this.installedVersions = installedVersions;
        this.approvals = approvals;
        this.policyService = policyService;
        this.evaluator = evaluator;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public DeploymentResponse schedule(ScheduleDeploymentRequest request, String correlationId) {
        AvailableRelease release = releases.findById(request.releaseId())
                .orElseThrow(() -> new ResourceNotFoundException("AvailableRelease", request.releaseId()));
        ReleaseEnvironment environment = environments.findById(request.environmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment", request.environmentId()));
        if (request.emergency() && (request.reason() == null || request.reason().isBlank())) {
            throw new GovernanceRuleException("EMERGENCY_REASON_REQUIRED", "Emergency rollout requires a reason.");
        }
        if (deployments.existsByReleaseIdAndEnvironmentIdAndStatusIn(release.getId(), environment.getId(), ACTIVE)) {
            throw new GovernanceRuleException("DEPLOYMENT_ALREADY_ACTIVE", "This release already has an active deployment in the environment.");
        }
        InstalledVersion installed = installedVersions.findByProductIdAndEnvironmentId(
                release.getProduct().getId(), environment.getId()).orElse(null);
        boolean approved = approvals.existsByReleaseIdAndEnvironmentIdAndStatus(
                release.getId(), environment.getId(), ApprovalStatus.APPROVED);
        ReleasePolicy policy = policyService.find(release.getProduct().getId());
        PolicyDecision decision = evaluator.evaluate(policy, environment, release,
                installed == null ? null : installed.getVersion(), approved, request.emergency(), clock.instant());
        if (!decision.allowed()) {
            throw new GovernanceRuleException("POLICY_VIOLATION", String.join(", ", decision.violations()));
        }

        if (release.getStatus() == ReleaseStatus.DISCOVERED) release.transitionTo(ReleaseStatus.APPROVED);
        if (release.getStatus() == ReleaseStatus.AWAITING_APPROVAL) release.transitionTo(ReleaseStatus.APPROVED);
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            throw new GovernanceRuleException("RELEASE_NOT_SCHEDULABLE", "Release must be approved before scheduling.");
        }
        release.transitionTo(ReleaseStatus.SCHEDULED);
        DeploymentRecord record = deployments.save(new DeploymentRecord(
                release.getProduct(), environment, release, installed == null ? null : installed.getVersion(),
                request.emergency(), request.actor().trim(), trim(request.reason())));
        audit.record(AuditAction.DEPLOYMENT_SCHEDULED, "DeploymentRecord", record.getId(), request.actor(),
                Map.of("releaseId", release.getId(), "environmentId", environment.getId(),
                        "emergency", request.emergency(), "bypasses", decision.bypasses()), correlationId);
        return toResponse(record, decision.warnings(), decision.bypasses());
    }

    @Transactional
    public DeploymentResponse complete(UUID deploymentId, CompleteDeploymentRequest request, String correlationId) {
        DeploymentRecord record = deployments.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("DeploymentRecord", deploymentId));
        record.complete(request.successful(), trim(request.failureReason()), clock.instant());
        AvailableRelease release = record.getRelease();
        if (request.successful()) {
            InstalledVersion installed = installedVersions.findByProductIdAndEnvironmentId(
                    record.getProduct().getId(), record.getEnvironment().getId()).orElse(null);
            if (installed == null) {
                installedVersions.save(new InstalledVersion(record.getProduct(), record.getEnvironment(),
                        record.getTargetVersion(), clock.instant(), request.actor().trim()));
            } else {
                installed.update(record.getTargetVersion(), clock.instant(), request.actor().trim());
            }
            release.transitionTo(record.getEnvironment().getType() == EnvironmentType.PRODUCTION
                    ? ReleaseStatus.DEPLOYED : ReleaseStatus.APPROVED);
            audit.record(AuditAction.DEPLOYMENT_SUCCEEDED, "DeploymentRecord", record.getId(), request.actor(),
                    Map.of("version", record.getTargetVersion(), "environmentId", record.getEnvironment().getId()), correlationId);
        } else {
            release.transitionTo(ReleaseStatus.APPROVED);
            audit.record(AuditAction.DEPLOYMENT_FAILED, "DeploymentRecord", record.getId(), request.actor(),
                    Map.of("version", record.getTargetVersion(), "failureReason", Objects.toString(request.failureReason(), "")), correlationId);
        }
        return toResponse(record, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public List<DeploymentResponse> history() {
        return deployments.findAllByOrderByScheduledAtDesc().stream()
                .map(value -> toResponse(value, List.of(), List.of())).toList();
    }

    private static DeploymentResponse toResponse(DeploymentRecord value, List<String> warnings, List<String> bypasses) {
        return new DeploymentResponse(value.getId(), value.getProduct().getId(), value.getProduct().getName(),
                value.getEnvironment().getId(), value.getEnvironment().getName(), value.getRelease().getId(),
                value.getPreviousVersion(), value.getTargetVersion(), value.getStatus(), value.isEmergency(),
                value.getActor(), value.getReason(), value.getScheduledAt(), value.getCompletedAt(),
                value.getFailureReason(), warnings, bypasses);
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
}
