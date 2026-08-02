CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    vendor VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('GITHUB_RELEASES', 'STATIC_JSON', 'MANUAL')),
    source_reference VARCHAR(500),
    default_channel VARCHAR(20) NOT NULL CHECK (default_channel IN ('STABLE', 'BETA', 'ALPHA', 'NIGHTLY')),
    last_check_status VARCHAR(24) NOT NULL CHECK (last_check_status IN ('NEVER_CHECKED', 'SUCCESS', 'FAILED', 'RATE_LIMITED')),
    last_checked_at TIMESTAMPTZ,
    last_check_error VARCHAR(1000),
    next_check_after TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_product_source_reference CHECK (
        source_type = 'MANUAL' OR (source_reference IS NOT NULL AND length(trim(source_reference)) > 0)
    )
);

CREATE TABLE environments (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION')),
    zone_id VARCHAR(60) NOT NULL,
    maintenance_start TIME,
    maintenance_end TIME,
    maintenance_days VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_maintenance_boundaries CHECK (
        (maintenance_start IS NULL AND maintenance_end IS NULL)
        OR (maintenance_start IS NOT NULL AND maintenance_end IS NOT NULL)
    )
);

CREATE TABLE available_releases (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    version VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('STABLE', 'BETA', 'ALPHA', 'NIGHTLY')),
    prerelease BOOLEAN NOT NULL,
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('GITHUB_RELEASES', 'STATIC_JSON', 'MANUAL')),
    source_external_id VARCHAR(180) NOT NULL,
    release_notes_url VARCHAR(500),
    notes VARCHAR(4000),
    published_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL CHECK (status IN (
        'DISCOVERED', 'AWAITING_APPROVAL', 'APPROVED', 'SCHEDULED', 'DEPLOYED', 'BLOCKED', 'SUPERSEDED'
    )),
    imported_at TIMESTAMPTZ NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_release_product_version_channel UNIQUE (product_id, version, channel),
    CONSTRAINT uq_release_product_source_external UNIQUE (product_id, source_type, source_external_id)
);

CREATE TABLE installed_versions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE RESTRICT,
    version VARCHAR(100) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(120) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_installed_product_environment UNIQUE (product_id, environment_id)
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    release_id UUID NOT NULL REFERENCES available_releases(id) ON DELETE RESTRICT,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED')),
    requested_by VARCHAR(120) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    decided_by VARCHAR(120),
    decided_at TIMESTAMPTZ,
    comment VARCHAR(1000)
);

CREATE TABLE deployment_records (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE RESTRICT,
    release_id UUID NOT NULL REFERENCES available_releases(id) ON DELETE RESTRICT,
    previous_version VARCHAR(100),
    target_version VARCHAR(100) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN (
        'SCHEDULED', 'IN_PROGRESS', 'SUCCESSFUL', 'FAILED', 'ROLLED_BACK', 'CANCELLED'
    )),
    emergency BOOLEAN NOT NULL,
    actor VARCHAR(120) NOT NULL,
    reason VARCHAR(1000),
    scheduled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    lock_version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE release_policies (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    prohibit_prerelease_in_production BOOLEAN NOT NULL DEFAULT TRUE,
    require_production_approval BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_supported_version VARCHAR(100),
    blocked_versions VARCHAR(1000) NOT NULL DEFAULT '',
    enforce_maintenance_window BOOLEAN NOT NULL DEFAULT TRUE,
    emergency_bypass_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_policy_product UNIQUE (product_id)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    action VARCHAR(40) NOT NULL CHECK (action IN (
        'PRODUCT_REGISTERED', 'RELEASE_IMPORTED', 'RELEASE_IMPORT_FAILED', 'RELEASE_RATE_LIMITED',
        'APPROVAL_REQUESTED', 'APPROVAL_GRANTED', 'APPROVAL_REJECTED', 'DEPLOYMENT_SCHEDULED',
        'DEPLOYMENT_SUCCEEDED', 'DEPLOYMENT_FAILED', 'DEPLOYMENT_ROLLED_BACK', 'POLICY_CHANGED', 'VERSION_BLOCKED'
    )),
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    actor VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(100)
);

CREATE INDEX ix_products_due_check ON products (active, next_check_after) WHERE source_type <> 'MANUAL';
CREATE INDEX ix_releases_product_published ON available_releases (product_id, published_at DESC);
CREATE INDEX ix_releases_status_channel ON available_releases (status, channel);
CREATE INDEX ix_installed_environment ON installed_versions (environment_id);
CREATE INDEX ix_approvals_release_environment ON approvals (release_id, environment_id, status);
CREATE UNIQUE INDEX uq_pending_approval ON approvals (release_id, environment_id) WHERE status = 'PENDING';
CREATE INDEX ix_deployments_product_environment_time ON deployment_records (product_id, environment_id, scheduled_at DESC);
CREATE UNIQUE INDEX uq_active_deployment ON deployment_records (product_id, environment_id)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');
CREATE INDEX ix_audit_aggregate_time ON audit_events (aggregate_type, aggregate_id, occurred_at DESC);
CREATE INDEX ix_audit_occurred_at ON audit_events (occurred_at DESC);

CREATE FUNCTION prevent_audit_event_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_append_only
BEFORE UPDATE OR DELETE ON audit_events
FOR EACH ROW EXECUTE FUNCTION prevent_audit_event_mutation();
