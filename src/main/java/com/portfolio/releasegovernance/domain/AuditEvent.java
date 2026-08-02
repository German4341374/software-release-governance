package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.AuditAction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id private UUID id = UUID.randomUUID();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AuditAction action;
    @Column(nullable = false, length = 80) private String aggregateType;
    @Column(nullable = false) private UUID aggregateId;
    @Column(nullable = false, length = 120) private String actor;
    @Column(nullable = false, updatable = false) private Instant occurredAt = Instant.now();
    @Column(nullable = false, columnDefinition = "jsonb") private String details = "{}";
    @Column(length = 100) private String correlationId;

    protected AuditEvent() {}

    public AuditEvent(AuditAction action, String aggregateType, UUID aggregateId, String actor,
                      Instant occurredAt, String details, String correlationId) {
        this.action = action;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actor = actor;
        this.occurredAt = occurredAt;
        this.details = details;
        this.correlationId = correlationId;
    }

    public UUID getId() { return id; }
    public AuditAction getAction() { return action; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDetails() { return details; }
    public String getCorrelationId() { return correlationId; }
}
