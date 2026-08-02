package com.portfolio.releasegovernance.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "release_policies", uniqueConstraints =
        @UniqueConstraint(name = "uq_policy_product", columnNames = "product_id"))
public class ReleasePolicy {
    @Id private UUID id = UUID.randomUUID();
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @Column(nullable = false) private boolean prohibitPrereleaseInProduction = true;
    @Column(nullable = false) private boolean requireProductionApproval = true;
    @Column(length = 100) private String minimumSupportedVersion;
    @Column(length = 1000) private String blockedVersions = "";
    @Column(nullable = false) private boolean enforceMaintenanceWindow = true;
    @Column(nullable = false) private boolean emergencyBypassAllowed = true;
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @Column(nullable = false, length = 120) private String updatedBy = "system";
    @Version private long lockVersion;

    protected ReleasePolicy() {}

    public ReleasePolicy(Product product) { this.product = product; }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public boolean isProhibitPrereleaseInProduction() { return prohibitPrereleaseInProduction; }
    public boolean isRequireProductionApproval() { return requireProductionApproval; }
    public String getMinimumSupportedVersion() { return minimumSupportedVersion; }
    public String getBlockedVersions() { return blockedVersions; }
    public boolean isEnforceMaintenanceWindow() { return enforceMaintenanceWindow; }
    public boolean isEmergencyBypassAllowed() { return emergencyBypassAllowed; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public long getLockVersion() { return lockVersion; }

    public void update(boolean prohibitPrerelease, boolean requireApproval, String minimumVersion,
                       String blockedVersions, boolean enforceWindow, boolean emergencyBypass,
                       String actor, Instant now) {
        this.prohibitPrereleaseInProduction = prohibitPrerelease;
        this.requireProductionApproval = requireApproval;
        this.minimumSupportedVersion = minimumVersion;
        this.blockedVersions = blockedVersions == null ? "" : blockedVersions;
        this.enforceMaintenanceWindow = enforceWindow;
        this.emergencyBypassAllowed = emergencyBypass;
        this.updatedBy = actor;
        this.updatedAt = now;
    }
}
