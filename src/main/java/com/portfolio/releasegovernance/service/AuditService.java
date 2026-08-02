package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.domain.DomainEnums.AuditAction;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.portfolio.releasegovernance.domain.AuditEvent;
import com.portfolio.releasegovernance.repository.AuditEventRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(AuditAction action, String aggregateType, UUID aggregateId, String actor,
                       Map<String, ?> details, String correlationId) {
        try {
            repository.save(new AuditEvent(action, aggregateType, aggregateId, actor, clock.instant(),
                    objectMapper.writeValueAsString(details), correlationId));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Audit details could not be serialized.", exception);
        }
    }
}
