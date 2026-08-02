package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.ApprovalStatus;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approvals")
public class Approval {
    @Id private UUID id = UUID.randomUUID();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "release_id") private AvailableRelease release;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "environment_id") private ReleaseEnvironment environment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ApprovalStatus status = ApprovalStatus.PENDING;
    @Column(nullable = false, length = 120) private String requestedBy;
    @Column(nullable = false, updatable = false) private Instant requestedAt = Instant.now();
    @Column(length = 120) private String decidedBy;
    private Instant decidedAt;
    @Column(length = 1000) private String comment;

    protected Approval() {}

    public Approval(AvailableRelease release, ReleaseEnvironment environment, String requestedBy, String comment) {
        this.release = release;
        this.environment = environment;
        this.requestedBy = requestedBy;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public AvailableRelease getRelease() { return release; }
    public ReleaseEnvironment getEnvironment() { return environment; }
    public ApprovalStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getComment() { return comment; }

    public void decide(ApprovalStatus decision, String actor, String decisionComment, Instant now) {
        if (status != ApprovalStatus.PENDING || decision == ApprovalStatus.PENDING) {
            throw new GovernanceRuleException("INVALID_APPROVAL_TRANSITION", "Only a pending approval can be decided.");
        }
        status = decision;
        decidedBy = actor;
        decidedAt = now;
        comment = decisionComment;
    }
}
