package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.AuditResponse;

import com.portfolio.releasegovernance.repository.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final AuditEventRepository repository;

    public AuditQueryService(AuditEventRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<AuditResponse> events(String aggregateType, UUID aggregateId, int limit) {
        return repository.findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
                        aggregateType, aggregateId, PageRequest.of(0, Math.clamp(limit, 1, 200))).stream()
                .map(value -> new AuditResponse(value.getId(), value.getAction(), value.getAggregateType(),
                        value.getAggregateId(), value.getActor(), value.getOccurredAt(),
                        value.getDetails(), value.getCorrelationId()))
                .toList();
    }
}
