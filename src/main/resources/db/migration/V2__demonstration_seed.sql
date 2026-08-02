INSERT INTO products (
    id, name, vendor, description, source_type, source_reference, default_channel,
    last_check_status, active, lock_version, created_at, updated_at
) VALUES (
    '10000000-0000-0000-0000-000000000001',
    'Operations Portal',
    'Example Systems',
    'Fictional product used to demonstrate controlled release promotion.',
    'MANUAL',
    NULL,
    'STABLE',
    'SUCCESS',
    TRUE,
    0,
    '2026-07-01T09:00:00Z',
    '2026-07-01T09:00:00Z'
);

INSERT INTO environments (id, name, type, zone_id, maintenance_start, maintenance_end, maintenance_days, active, created_at) VALUES
('20000000-0000-0000-0000-000000000001', 'Development', 'DEVELOPMENT', 'UTC', NULL, NULL, NULL, TRUE, '2026-07-01T09:00:00Z'),
('20000000-0000-0000-0000-000000000002', 'Staging', 'STAGING', 'UTC', '00:00', '23:59', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY', TRUE, '2026-07-01T09:00:00Z'),
('20000000-0000-0000-0000-000000000003', 'Production', 'PRODUCTION', 'UTC', '01:00', '05:00', 'TUESDAY,THURSDAY', TRUE, '2026-07-01T09:00:00Z');

INSERT INTO available_releases (
    id, product_id, version, channel, prerelease, source_type, source_external_id,
    release_notes_url, notes, published_at, status, imported_at, lock_version
) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '1.1.0', 'STABLE', FALSE, 'MANUAL', 'seed:1.1.0', NULL, 'Previous supported release.', '2026-03-01T09:00:00Z', 'SUPERSEDED', '2026-03-01T09:00:00Z', 0),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '1.2.0', 'STABLE', FALSE, 'MANUAL', 'seed:1.2.0', NULL, 'Minimum supported release.', '2026-05-01T09:00:00Z', 'APPROVED', '2026-05-01T09:00:00Z', 0),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '1.4.0', 'STABLE', FALSE, 'MANUAL', 'seed:1.4.0', NULL, 'Current release awaiting production approval.', '2026-07-15T09:00:00Z', 'AWAITING_APPROVAL', '2026-07-15T09:00:00Z', 0),
('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '2.0.0-beta.1', 'BETA', TRUE, 'MANUAL', 'seed:2.0.0-beta.1', NULL, 'Prerelease blocked in production by policy.', '2026-07-25T09:00:00Z', 'DISCOVERED', '2026-07-25T09:00:00Z', 0);

INSERT INTO installed_versions (id, product_id, environment_id, version, installed_at, recorded_by, lock_version) VALUES
('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '1.4.0', '2026-07-20T10:00:00Z', 'seed', 0),
('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '1.2.0', '2026-06-01T10:00:00Z', 'seed', 0),
('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000003', '1.1.0', '2026-03-05T10:00:00Z', 'seed', 0);

INSERT INTO release_policies (
    id, product_id, prohibit_prerelease_in_production, require_production_approval,
    minimum_supported_version, blocked_versions, enforce_maintenance_window,
    emergency_bypass_allowed, updated_at, updated_by, lock_version
) VALUES (
    '50000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    TRUE,
    TRUE,
    '1.2.0',
    '1.3.4,2.0.*',
    TRUE,
    TRUE,
    '2026-07-01T09:00:00Z',
    'seed',
    0
);

INSERT INTO approvals (
    id, release_id, environment_id, status, requested_by, requested_at, comment
) VALUES (
    '60000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000003',
    'PENDING',
    'release-manager',
    '2026-07-28T09:00:00Z',
    'Validate production rollout evidence.'
);

INSERT INTO deployment_records (
    id, product_id, environment_id, release_id, previous_version, target_version,
    status, emergency, actor, reason, scheduled_at, completed_at, lock_version
) VALUES (
    '70000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    '1.0.0',
    '1.1.0',
    'SUCCESSFUL',
    FALSE,
    'seed',
    'Demonstration deployment history.',
    '2026-03-05T09:30:00Z',
    '2026-03-05T10:00:00Z',
    0
);

INSERT INTO audit_events (id, action, aggregate_type, aggregate_id, actor, occurred_at, details, correlation_id) VALUES
('80000000-0000-0000-0000-000000000001', 'PRODUCT_REGISTERED', 'Product', '10000000-0000-0000-0000-000000000001', 'seed', '2026-07-01T09:00:00Z', '{"source":"demonstration seed"}', 'seed-001'),
('80000000-0000-0000-0000-000000000002', 'APPROVAL_REQUESTED', 'Approval', '60000000-0000-0000-0000-000000000001', 'release-manager', '2026-07-28T09:00:00Z', '{"version":"1.4.0","environment":"Production"}', 'seed-002');
