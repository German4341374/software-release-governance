package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.ReleasePolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleasePolicyRepository extends JpaRepository<ReleasePolicy, UUID> {
    Optional<ReleasePolicy> findByProductId(UUID productId);
}
