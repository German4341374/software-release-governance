package com.portfolio.releasegovernance.repository;

import static com.portfolio.releasegovernance.domain.DomainEnums.ApprovalStatus;

import com.portfolio.releasegovernance.domain.Approval;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    long countByStatus(ApprovalStatus status);
    boolean existsByReleaseIdAndEnvironmentIdAndStatus(UUID releaseId, UUID environmentId, ApprovalStatus status);
    Optional<Approval> findFirstByReleaseIdAndEnvironmentIdAndStatusOrderByRequestedAtDesc(
            UUID releaseId, UUID environmentId, ApprovalStatus status);

    @EntityGraph(attributePaths = {"release", "release.product", "environment"})
    List<Approval> findAllByOrderByRequestedAtDesc();
}
