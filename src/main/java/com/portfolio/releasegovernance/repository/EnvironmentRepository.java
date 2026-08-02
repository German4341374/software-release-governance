package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.ReleaseEnvironment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<ReleaseEnvironment, UUID> {
    Optional<ReleaseEnvironment> findByNameIgnoreCase(String name);
}
