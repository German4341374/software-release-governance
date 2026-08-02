package com.portfolio.releasegovernance.service;

import com.portfolio.releasegovernance.adapter.VersionSourceException;
import com.portfolio.releasegovernance.repository.ProductRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "governance.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReleaseCheckScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReleaseCheckScheduler.class);
    private final ProductRepository products;
    private final ReleaseImportService imports;
    private final Clock clock;

    public ReleaseCheckScheduler(ProductRepository products, ReleaseImportService imports, Clock clock) {
        this.products = products;
        this.imports = imports;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${governance.scheduler.fixed-delay:PT30M}")
    public void refreshDueProducts() {
        for (UUID productId : products.findProductsDueForCheck(clock.instant())) {
            try {
                imports.refresh(productId, "release-scheduler", "scheduled:" + clock.instant());
            } catch (VersionSourceException exception) {
                log.warn("Release check failed for productId={} rateLimited={}", productId, exception.isRateLimited());
            } catch (RuntimeException exception) {
                log.error("Unexpected scheduled release check failure for productId={}", productId, exception);
            }
        }
    }
}
