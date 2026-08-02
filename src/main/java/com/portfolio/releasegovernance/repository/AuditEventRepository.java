package com.portfolio.releasegovernance.repository;

import com.portfolio.releasegovernance.domain.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
            String aggregateType, UUID aggregateId, Pageable pageable);
}
