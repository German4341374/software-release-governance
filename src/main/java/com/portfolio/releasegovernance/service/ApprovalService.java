package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.*;
import com.portfolio.releasegovernance.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final ApprovalRepository approvals;
    private final AvailableReleaseRepository releases;
    private final EnvironmentRepository environments;
    private final AuditService audit;
    private final Clock clock;

    public ApprovalService(ApprovalRepository approvals, AvailableReleaseRepository releases,
                           EnvironmentRepository environments, AuditService audit, Clock clock) {
        this.approvals = approvals;
        this.releases = releases;
        this.environments = environments;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ApprovalResponse request(RequestApprovalRequest request, String correlationId) {
        Optional<Approval> existing = approvals.findFirstByReleaseIdAndEnvironmentIdAndStatusOrderByRequestedAtDesc(
                request.releaseId(), request.environmentId(), ApprovalStatus.PENDING);
        if (existing.isPresent()) return toResponse(existing.get());
        AvailableRelease release = findRelease(request.releaseId());
        ReleaseEnvironment environment = findEnvironment(request.environmentId());
        Approval approval = approvals.save(new Approval(release, environment, request.requestedBy().trim(), trim(request.comment())));
        if (release.getStatus() == ReleaseStatus.DISCOVERED) release.transitionTo(ReleaseStatus.AWAITING_APPROVAL);
        audit.record(AuditAction.APPROVAL_REQUESTED, "Approval", approval.getId(), request.requestedBy(),
                Map.of("releaseId", release.getId(), "environmentId", environment.getId()), correlationId);
        return toResponse(approval);
    }

    @Transactional
    public ApprovalResponse decide(UUID approvalId, DecideApprovalRequest request, String correlationId) {
        if (request.decision() != ApprovalStatus.APPROVED && request.decision() != ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Approval decision must be APPROVED or REJECTED.");
        }
        Approval approval = approvals.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval", approvalId));
        approval.decide(request.decision(), request.actor().trim(), trim(request.comment()), clock.instant());
        AvailableRelease release = approval.getRelease();
        if (request.decision() == ApprovalStatus.APPROVED && release.getStatus() == ReleaseStatus.AWAITING_APPROVAL) {
            release.transitionTo(ReleaseStatus.APPROVED);
        }
        if (request.decision() == ApprovalStatus.REJECTED
                && (release.getStatus() == ReleaseStatus.AWAITING_APPROVAL || release.getStatus() == ReleaseStatus.APPROVED)) {
            release.transitionTo(ReleaseStatus.BLOCKED);
        }
        AuditAction action = request.decision() == ApprovalStatus.APPROVED
                ? AuditAction.APPROVAL_GRANTED : AuditAction.APPROVAL_REJECTED;
        audit.record(action, "Approval", approval.getId(), request.actor(),
                Map.of("releaseId", release.getId(), "decision", request.decision()), correlationId);
        return toResponse(approval);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> list() {
        return approvals.findAllByOrderByRequestedAtDesc().stream().map(ApprovalService::toResponse).toList();
    }

    private AvailableRelease findRelease(UUID id) {
        return releases.findById(id).orElseThrow(() -> new ResourceNotFoundException("AvailableRelease", id));
    }

    private ReleaseEnvironment findEnvironment(UUID id) {
        return environments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Environment", id));
    }

    static ApprovalResponse toResponse(Approval value) {
        return new ApprovalResponse(value.getId(), value.getRelease().getId(), value.getRelease().getVersion(),
                value.getEnvironment().getId(), value.getEnvironment().getName(), value.getStatus(),
                value.getRequestedBy(), value.getRequestedAt(), value.getDecidedBy(), value.getDecidedAt(), value.getComment());
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
}
