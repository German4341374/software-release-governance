package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.adapter.*;
import com.portfolio.releasegovernance.domain.Product;
import com.portfolio.releasegovernance.policy.SemanticVersion;
import com.portfolio.releasegovernance.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseImportService {
    private final ProductRepository products;
    private final AvailableReleaseRepository releases;
    private final VersionSourceRegistry adapters;
    private final ReleasePersistenceService persistence;
    private final Clock clock;

    public ReleaseImportService(ProductRepository products, AvailableReleaseRepository releases,
                                VersionSourceRegistry adapters, ReleasePersistenceService persistence, Clock clock) {
        this.products = products;
        this.releases = releases;
        this.adapters = adapters;
        this.persistence = persistence;
        this.clock = clock;
    }

    public ImportSummary refresh(UUID productId, String actor, String correlationId) {
        Product product = products.findById(productId).orElseThrow(() -> new com.portfolio.releasegovernance.domain.ResourceNotFoundException("Product", productId));
        try {
            List<ReleaseCandidate> candidates = adapters.adapterFor(product.getSourceType()).fetch(product);
            return persistence.persist(productId, candidates, actor, correlationId);
        } catch (VersionSourceException exception) {
            persistence.recordFailure(productId, exception.getMessage(), exception.isRateLimited(),
                    exception.getRetryAfter(), actor, correlationId);
            throw exception;
        }
    }

    public ImportSummary addManual(UUID productId, ManualReleaseRequest request, String actor, String correlationId) {
        String version = SemanticVersion.parse(request.version()).canonical();
        ReleaseCandidate candidate = new ReleaseCandidate(
                version, request.channel(), SemanticVersion.parse(version).isPrerelease(), SourceType.MANUAL,
                request.externalId() == null || request.externalId().isBlank() ? "manual:" + version + ":" + request.channel() : request.externalId(),
                request.releaseNotesUrl(), request.notes(), request.publishedAt() == null ? clock.instant() : request.publishedAt());
        return persistence.persist(productId, List.of(candidate), actor, correlationId);
    }

    @Transactional(readOnly = true)
    public List<ReleaseResponse> releases(UUID productId) {
        return releases.findByProductIdOrderByPublishedAtDesc(productId).stream().map(ReleaseImportService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReleaseResponse release(UUID id) {
        return toResponse(releases.findById(id).orElseThrow(() -> new com.portfolio.releasegovernance.domain.ResourceNotFoundException("AvailableRelease", id)));
    }

    static ReleaseResponse toResponse(com.portfolio.releasegovernance.domain.AvailableRelease value) {
        return new ReleaseResponse(value.getId(), value.getProduct().getId(), value.getProduct().getName(),
                value.getVersion(), value.getChannel(), value.isPrerelease(), value.getSourceType(),
                value.getSourceExternalId(), value.getReleaseNotesUrl(), value.getNotes(),
                value.getPublishedAt(), value.getStatus(), value.getLockVersion());
    }
}
