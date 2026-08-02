package com.portfolio.releasegovernance.domain;

public final class DomainEnums {

    private DomainEnums() {}

    public enum SourceType { GITHUB_RELEASES, STATIC_JSON, MANUAL }

    public enum ReleaseChannel { STABLE, BETA, ALPHA, NIGHTLY }

    public enum CheckStatus { NEVER_CHECKED, SUCCESS, FAILED, RATE_LIMITED }

    public enum EnvironmentType { DEVELOPMENT, STAGING, PRODUCTION }

    public enum ReleaseStatus {
        DISCOVERED,
        AWAITING_APPROVAL,
        APPROVED,
        SCHEDULED,
        DEPLOYED,
        BLOCKED,
        SUPERSEDED
    }

    public enum ApprovalStatus { PENDING, APPROVED, REJECTED, REVOKED }

    public enum DeploymentStatus { SCHEDULED, IN_PROGRESS, SUCCESSFUL, FAILED, ROLLED_BACK, CANCELLED }

    public enum AuditAction {
        PRODUCT_REGISTERED,
        RELEASE_IMPORTED,
        RELEASE_IMPORT_FAILED,
        RELEASE_RATE_LIMITED,
        APPROVAL_REQUESTED,
        APPROVAL_GRANTED,
        APPROVAL_REJECTED,
        DEPLOYMENT_SCHEDULED,
        DEPLOYMENT_SUCCEEDED,
        DEPLOYMENT_FAILED,
        DEPLOYMENT_ROLLED_BACK,
        POLICY_CHANGED,
        VERSION_BLOCKED
    }
}
