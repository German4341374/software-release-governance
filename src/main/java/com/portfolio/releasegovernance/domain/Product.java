package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 120) private String name;
    @Column(nullable = false, length = 120) private String vendor;
    @Column(length = 1000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private SourceType sourceType;
    @Column(length = 500) private String sourceReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReleaseChannel defaultChannel = ReleaseChannel.STABLE;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private CheckStatus lastCheckStatus = CheckStatus.NEVER_CHECKED;
    private Instant lastCheckedAt;
    @Column(length = 1000) private String lastCheckError;
    private Instant nextCheckAfter;
    @Column(nullable = false) private boolean active = true;
    @Version private long lockVersion;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected Product() {}

    public Product(String name, String vendor, String description, SourceType sourceType,
                   String sourceReference, ReleaseChannel defaultChannel) {
        this.name = name;
        this.vendor = vendor;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceReference = sourceReference;
        this.defaultChannel = defaultChannel;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getVendor() { return vendor; }
    public String getDescription() { return description; }
    public SourceType getSourceType() { return sourceType; }
    public String getSourceReference() { return sourceReference; }
    public ReleaseChannel getDefaultChannel() { return defaultChannel; }
    public CheckStatus getLastCheckStatus() { return lastCheckStatus; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public String getLastCheckError() { return lastCheckError; }
    public Instant getNextCheckAfter() { return nextCheckAfter; }
    public boolean isActive() { return active; }
    public long getLockVersion() { return lockVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void recordSuccessfulCheck(Instant now) {
        lastCheckStatus = CheckStatus.SUCCESS;
        lastCheckedAt = now;
        lastCheckError = null;
        nextCheckAfter = null;
        updatedAt = now;
    }

    public void recordFailedCheck(Instant now, String safeError, Instant retryAfter, boolean rateLimited) {
        lastCheckStatus = rateLimited ? CheckStatus.RATE_LIMITED : CheckStatus.FAILED;
        lastCheckedAt = now;
        lastCheckError = safeError;
        nextCheckAfter = retryAfter;
        updatedAt = now;
    }
}
