# REST API reference

All write endpoints accept JSON and return JSON. Supply `X-Actor` where supported and a reusable `X-Correlation-ID` to connect audit evidence to an external change record. Validation and policy failures use `application/problem+json`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Lightweight health response |
| GET/POST | `/api/products` | List or register products |
| GET | `/api/products/{id}` | Product details and source state |
| POST | `/api/products/{id}/refresh` | Run its configured source adapter |
| GET/POST | `/api/products/{id}/releases` | List or manually import releases |
| GET/POST | `/api/environments` | List or register environments |
| GET/PUT | `/api/products/{id}/policy` | Read or update the product policy |
| GET/POST | `/api/approvals` | List or request approvals |
| POST | `/api/approvals/{id}/decision` | Approve or reject a pending request |
| GET/POST | `/api/deployments` | List history or schedule a deployment |
| POST | `/api/deployments/{id}/complete` | Record success or failure |
| GET | `/api/dashboard` | Product/environment version drift |
| GET | `/api/audit/{type}/{id}?limit=100` | Read aggregate audit events |

## Approval decision

```json
{
  "decision": "APPROVED",
  "actor": "change-advisory-board",
  "comment": "Change CHG-1042 approved after staging evidence review."
}
```

The `decision` value is `APPROVED` or `REJECTED`. A decision is scoped to one release and one environment.

## Schedule deployment

```json
{
  "releaseId": "30000000-0000-0000-0000-000000000003",
  "environmentId": "20000000-0000-0000-0000-000000000003",
  "emergency": false,
  "actor": "release-manager",
  "reason": "Approved production maintenance window"
}
```

Emergency requests require a reason. A successful response is a governance record, not proof that an external deployment was executed.

## Complete deployment

```json
{
  "successful": true,
  "actor": "deployment-pipeline",
  "failureReason": null
}
```

On success, the environment's installed version is upserted atomically. On failure, the release returns to its prior deployable state while the failed record remains in history.

## Update policy with optimistic concurrency

```json
{
  "prohibitPrereleaseInProduction": true,
  "requireProductionApproval": true,
  "minimumSupportedVersion": "1.2.0",
  "blockedVersions": "1.3.4,2.0.*",
  "enforceMaintenanceWindow": true,
  "emergencyBypassAllowed": true,
  "actor": "release-governance-owner",
  "expectedVersion": 0
}
```

Read the current `lockVersion` first. A stale `expectedVersion` returns HTTP 409, preventing silent lost updates.
