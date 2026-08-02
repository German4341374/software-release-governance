package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.EnvironmentType;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "environments")
public class ReleaseEnvironment {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EnvironmentType type;
    @Column(nullable = false, length = 60) private String zoneId = "UTC";
    private LocalTime maintenanceStart;
    private LocalTime maintenanceEnd;
    @Column(length = 40) private String maintenanceDays;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected ReleaseEnvironment() {}

    public ReleaseEnvironment(String name, EnvironmentType type, String zoneId,
                              LocalTime maintenanceStart, LocalTime maintenanceEnd, String maintenanceDays) {
        this.name = name;
        this.type = type;
        this.zoneId = zoneId;
        this.maintenanceStart = maintenanceStart;
        this.maintenanceEnd = maintenanceEnd;
        this.maintenanceDays = maintenanceDays;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public EnvironmentType getType() { return type; }
    public String getZoneId() { return zoneId; }
    public LocalTime getMaintenanceStart() { return maintenanceStart; }
    public LocalTime getMaintenanceEnd() { return maintenanceEnd; }
    public String getMaintenanceDays() { return maintenanceDays; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
