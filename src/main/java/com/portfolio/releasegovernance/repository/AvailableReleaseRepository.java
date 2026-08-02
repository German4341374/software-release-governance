package com.portfolio.releasegovernance.repository;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.AvailableRelease;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailableReleaseRepository extends JpaRepository<AvailableRelease, UUID> {
    Optional<AvailableRelease> findByProductIdAndVersionAndChannel(UUID productId, String version, ReleaseChannel channel);
    boolean existsByProductIdAndSourceTypeAndSourceExternalId(UUID productId, SourceType sourceType, String sourceExternalId);

    @EntityGraph(attributePaths = "product")
    List<AvailableRelease> findByProductIdOrderByPublishedAtDesc(UUID productId);

    @EntityGraph(attributePaths = "product")
    List<AvailableRelease> findAllByOrderByImportedAtDesc();
}
