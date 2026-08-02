package com.portfolio.releasegovernance.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "installed_versions", uniqueConstraints =
        @UniqueConstraint(name = "uq_installed_product_environment", columnNames = {"product_id", "environment_id"}))
public class InstalledVersion {
    @Id private UUID id = UUID.randomUUID();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "environment_id") private ReleaseEnvironment environment;
    @Column(nullable = false, length = 100) private String version;
    @Column(nullable = false) private Instant installedAt;
    @Column(nullable = false, length = 120) private String recordedBy;
    @Version private long lockVersion;

    protected InstalledVersion() {}

    public InstalledVersion(Product product, ReleaseEnvironment environment, String version,
                            Instant installedAt, String recordedBy) {
        this.product = product;
        this.environment = environment;
        this.version = version;
        this.installedAt = installedAt;
        this.recordedBy = recordedBy;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public ReleaseEnvironment getEnvironment() { return environment; }
    public String getVersion() { return version; }
    public Instant getInstalledAt() { return installedAt; }
    public String getRecordedBy() { return recordedBy; }
    public long getLockVersion() { return lockVersion; }

    public void update(String version, Instant installedAt, String recordedBy) {
        this.version = version;
        this.installedAt = installedAt;
        this.recordedBy = recordedBy;
    }
}
