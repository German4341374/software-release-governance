package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.InstalledVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstalledVersionRepository extends JpaRepository<InstalledVersion, UUID> {
    Optional<InstalledVersion> findByProductIdAndEnvironmentId(UUID productId, UUID environmentId);

    @EntityGraph(attributePaths = {"product", "environment"})
    List<InstalledVersion> findAllByOrderByProductNameAscEnvironmentNameAsc();
}
