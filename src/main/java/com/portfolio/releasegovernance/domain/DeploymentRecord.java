package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.DeploymentStatus;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deployment_records")
public class DeploymentRecord {
    @Id private UUID id = UUID.randomUUID();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "environment_id") private ReleaseEnvironment environment;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "release_id") private AvailableRelease release;
    @Column(length = 100) private String previousVersion;
    @Column(nullable = false, length = 100) private String targetVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private DeploymentStatus status = DeploymentStatus.SCHEDULED;
    @Column(nullable = false) private boolean emergency;
    @Column(nullable = false, length = 120) private String actor;
    @Column(length = 1000) private String reason;
    @Column(nullable = false, updatable = false) private Instant scheduledAt = Instant.now();
    private Instant completedAt;
    @Column(length = 1000) private String failureReason;
    @Version private long lockVersion;

    protected DeploymentRecord() {}

    public DeploymentRecord(Product product, ReleaseEnvironment environment, AvailableRelease release,
                            String previousVersion, boolean emergency, String actor, String reason) {
        this.product = product;
        this.environment = environment;
        this.release = release;
        this.previousVersion = previousVersion;
        this.targetVersion = release.getVersion();
        this.emergency = emergency;
        this.actor = actor;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public ReleaseEnvironment getEnvironment() { return environment; }
    public AvailableRelease getRelease() { return release; }
    public String getPreviousVersion() { return previousVersion; }
    public String getTargetVersion() { return targetVersion; }
    public DeploymentStatus getStatus() { return status; }
    public boolean isEmergency() { return emergency; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
    public long getLockVersion() { return lockVersion; }

    public void start() {
        if (status != DeploymentStatus.SCHEDULED) throw new GovernanceRuleException("INVALID_DEPLOYMENT_TRANSITION", "Only scheduled deployments can start.");
        status = DeploymentStatus.IN_PROGRESS;
    }

    public void complete(boolean successful, String failureReason, Instant now) {
        if (status != DeploymentStatus.SCHEDULED && status != DeploymentStatus.IN_PROGRESS) {
            throw new GovernanceRuleException("INVALID_DEPLOYMENT_TRANSITION", "Deployment is not active.");
        }
        status = successful ? DeploymentStatus.SUCCESSFUL : DeploymentStatus.FAILED;
        this.failureReason = successful ? null : failureReason;
        completedAt = now;
    }
}
