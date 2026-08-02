package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "available_releases", uniqueConstraints = {
        @UniqueConstraint(name = "uq_release_product_version_channel", columnNames = {"product_id", "version", "channel"}),
        @UniqueConstraint(name = "uq_release_product_source_external", columnNames = {"product_id", "source_type", "source_external_id"})
})
public class AvailableRelease {
    @Id private UUID id = UUID.randomUUID();
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @Column(nullable = false, length = 100) private String version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReleaseChannel channel;
    @Column(nullable = false) private boolean prerelease;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private SourceType sourceType;
    @Column(nullable = false, length = 180) private String sourceExternalId;
    @Column(length = 500) private String releaseNotesUrl;
    @Column(length = 4000) private String notes;
    private Instant publishedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private ReleaseStatus status = ReleaseStatus.DISCOVERED;
    @Column(nullable = false, updatable = false) private Instant importedAt = Instant.now();
    @Version private long lockVersion;

    protected AvailableRelease() {}

    public AvailableRelease(Product product, String version, ReleaseChannel channel, boolean prerelease,
                            SourceType sourceType, String sourceExternalId, String releaseNotesUrl,
                            String notes, Instant publishedAt) {
        this.product = product;
        this.version = version;
        this.channel = channel;
        this.prerelease = prerelease;
        this.sourceType = sourceType;
        this.sourceExternalId = sourceExternalId;
        this.releaseNotesUrl = releaseNotesUrl;
        this.notes = notes;
        this.publishedAt = publishedAt;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public String getVersion() { return version; }
    public ReleaseChannel getChannel() { return channel; }
    public boolean isPrerelease() { return prerelease; }
    public SourceType getSourceType() { return sourceType; }
    public String getSourceExternalId() { return sourceExternalId; }
    public String getReleaseNotesUrl() { return releaseNotesUrl; }
    public String getNotes() { return notes; }
    public Instant getPublishedAt() { return publishedAt; }
    public ReleaseStatus getStatus() { return status; }
    public Instant getImportedAt() { return importedAt; }
    public long getLockVersion() { return lockVersion; }
    public void transitionTo(ReleaseStatus next) { ReleaseStateMachine.ensureAllowed(status, next); status = next; }
}
