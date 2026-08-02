package com.portfolio.releasegovernance.api;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
import java.util.UUID;

public final class ApiContracts {
    private ApiContracts() {}

    public record CreateProductRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String vendor,
            @Size(max = 1000) String description,
            @NotNull SourceType sourceType,
            @Size(max = 500) String sourceReference,
            @NotNull ReleaseChannel defaultChannel) {}

    public record ProductResponse(
            UUID id, String name, String vendor, String description, SourceType sourceType,
            String sourceReference, ReleaseChannel defaultChannel, CheckStatus lastCheckStatus,
            Instant lastCheckedAt, String lastCheckError, Instant nextCheckAfter, boolean active, long lockVersion) {}

    public record CreateEnvironmentRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull EnvironmentType type,
            @NotBlank @Size(max = 60) String zoneId,
            LocalTime maintenanceStart,
            LocalTime maintenanceEnd,
            @Size(max = 40) String maintenanceDays) {}

    public record EnvironmentResponse(
            UUID id, String name, EnvironmentType type, String zoneId,
            LocalTime maintenanceStart, LocalTime maintenanceEnd, String maintenanceDays) {}

    public record ManualReleaseRequest(
            @NotBlank @Size(max = 100) String version,
            @NotNull ReleaseChannel channel,
            @Size(max = 180) String externalId,
            @Size(max = 500) String releaseNotesUrl,
            @Size(max = 4000) String notes,
            Instant publishedAt) {}

    public record ReleaseResponse(
            UUID id, UUID productId, String product, String version, ReleaseChannel channel,
            boolean prerelease, SourceType sourceType, String sourceExternalId,
            String releaseNotesUrl, String notes, Instant publishedAt, ReleaseStatus status, long lockVersion) {}

    public record ImportSummary(UUID productId, int discovered, int imported, int skipped, Instant checkedAt) {}

    public record RequestApprovalRequest(
            @NotNull UUID releaseId,
            @NotNull UUID environmentId,
            @NotBlank @Size(max = 120) String requestedBy,
            @Size(max = 1000) String comment) {}

    public record DecideApprovalRequest(
            @NotNull ApprovalStatus decision,
            @NotBlank @Size(max = 120) String actor,
            @Size(max = 1000) String comment) {}

    public record ApprovalResponse(
            UUID id, UUID releaseId, String version, UUID environmentId, String environment,
            ApprovalStatus status, String requestedBy, Instant requestedAt,
            String decidedBy, Instant decidedAt, String comment) {}

    public record ScheduleDeploymentRequest(
            @NotNull UUID releaseId,
            @NotNull UUID environmentId,
            boolean emergency,
            @NotBlank @Size(max = 120) String actor,
            @Size(max = 1000) String reason) {}

    public record CompleteDeploymentRequest(
            boolean successful,
            @NotBlank @Size(max = 120) String actor,
            @Size(max = 1000) String failureReason) {}

    public record DeploymentResponse(
            UUID id, UUID productId, String product, UUID environmentId, String environment,
            UUID releaseId, String previousVersion, String targetVersion, DeploymentStatus status,
            boolean emergency, String actor, String reason, Instant scheduledAt,
            Instant completedAt, String failureReason, List<String> policyWarnings, List<String> bypasses) {}

    public record UpdatePolicyRequest(
            boolean prohibitPrereleaseInProduction,
            boolean requireProductionApproval,
            @Size(max = 100) String minimumSupportedVersion,
            @Size(max = 1000) String blockedVersions,
            boolean enforceMaintenanceWindow,
            boolean emergencyBypassAllowed,
            @NotBlank @Size(max = 120) String actor,
            long expectedVersion) {}

    public record PolicyResponse(
            UUID id, UUID productId, boolean prohibitPrereleaseInProduction,
            boolean requireProductionApproval, String minimumSupportedVersion,
            String blockedVersions, boolean enforceMaintenanceWindow,
            boolean emergencyBypassAllowed, Instant updatedAt, String updatedBy, long lockVersion) {}

    public record OutdatedEnvironment(
            UUID productId, String product, UUID environmentId, String environment,
            String installedVersion, String latestVersion, boolean outdated) {}

    public record DashboardResponse(
            long products, long environments, long availableReleases, long pendingApprovals,
            long failedDeployments, List<OutdatedEnvironment> versionStatus) {}

    public record AuditResponse(
            UUID id, AuditAction action, String aggregateType, UUID aggregateId,
            String actor, Instant occurredAt, String details, String correlationId) {}
}
