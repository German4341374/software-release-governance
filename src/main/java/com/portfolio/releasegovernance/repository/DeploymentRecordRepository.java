package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.DeploymentRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRecordRepository extends JpaRepository<DeploymentRecord, UUID> {
    long countByStatus(com.portfolio.releasegovernance.domain.DomainEnums.DeploymentStatus status);
    @EntityGraph(attributePaths = {"product", "environment", "release"})
    List<DeploymentRecord> findAllByOrderByScheduledAtDesc();

    boolean existsByReleaseIdAndEnvironmentIdAndStatusIn(UUID releaseId, UUID environmentId,
                                                          java.util.Collection<com.portfolio.releasegovernance.domain.DomainEnums.DeploymentStatus> statuses);
}
