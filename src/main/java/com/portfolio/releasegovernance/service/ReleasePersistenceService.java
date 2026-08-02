package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.ImportSummary;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.adapter.ReleaseCandidate;
import com.portfolio.releasegovernance.domain.*;
import com.portfolio.releasegovernance.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleasePersistenceService {
    private final ProductRepository products;
    private final AvailableReleaseRepository releases;
    private final AuditService audit;
    private final Clock clock;

    public ReleasePersistenceService(ProductRepository products, AvailableReleaseRepository releases,
                                     AuditService audit, Clock clock) {
        this.products = products;
        this.releases = releases;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ImportSummary persist(UUID productId, List<ReleaseCandidate> candidates, String actor, String correlationId) {
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        int imported = 0;
        int skipped = 0;
        for (ReleaseCandidate candidate : candidates) {
            boolean exists = releases.existsByProductIdAndSourceTypeAndSourceExternalId(
                    productId, candidate.sourceType(), candidate.externalId())
                    || releases.findByProductIdAndVersionAndChannel(productId, candidate.version(), candidate.channel()).isPresent();
            if (exists) {
                skipped++;
                continue;
            }
            AvailableRelease release = releases.save(new AvailableRelease(
                    product, candidate.version(), candidate.channel(), candidate.prerelease(), candidate.sourceType(),
                    candidate.externalId(), candidate.releaseNotesUrl(), candidate.notes(), candidate.publishedAt()));
            imported++;
            audit.record(AuditAction.RELEASE_IMPORTED, "AvailableRelease", release.getId(), actor,
                    Map.of("productId", productId, "version", release.getVersion(), "channel", release.getChannel()), correlationId);
        }
        Instant now = clock.instant();
        product.recordSuccessfulCheck(now);
        return new ImportSummary(productId, candidates.size(), imported, skipped, now);
    }

    @Transactional
    public void recordFailure(UUID productId, String safeMessage, boolean rateLimited,
                              Instant retryAfter, String actor, String correlationId) {
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Instant now = clock.instant();
        product.recordFailedCheck(now, safeMessage, retryAfter, rateLimited);
        audit.record(rateLimited ? AuditAction.RELEASE_RATE_LIMITED : AuditAction.RELEASE_IMPORT_FAILED,
                "Product", productId, actor,
                Map.of("message", safeMessage, "retryAfter", Objects.toString(retryAfter, "")), correlationId);
    }
}
