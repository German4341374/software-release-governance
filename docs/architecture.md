# Architecture and decisions

## Boundaries

The service uses a modular monolith. HTTP controllers translate requests, application services coordinate use cases, pure domain components enforce policy and lifecycle rules, adapters normalize external release metadata, and repositories own persistence. This keeps deployment simple while preserving boundaries that can be tested independently.

## Adapter architecture

`VersionSourceAdapter` is the stable port:

```java
SourceType sourceType();
List<ReleaseCandidate> fetch(Product product);
```

`VersionSourceRegistry` selects one adapter from the product's source type. GitHub and static JSON implementations share `ResilientJsonClient`, so URL validation, timeout, retry, rate-limit interpretation, response parsing, and secret-safe logging are consistent. The manual implementation performs no network I/O; manual candidates enter through the same persistence workflow.

Adapters return a small internal `ReleaseCandidate` record. No GitHub- or JSON-specific response object crosses into the service layer. A new source requires an enum value, one adapter, contract tests, and any source-specific validation—not changes to policy evaluation.

## Transaction boundaries

| Use case | Transaction boundary | Reason |
|---|---|---|
| Scheduled or manual import | Fetch outside transaction; persist candidates, product check status, and audit records in one short transaction | A slow source must not hold a connection or row locks |
| Request or decide approval | Approval mutation, release transition, and audit event in one transaction | Evidence cannot diverge from state |
| Schedule deployment | Lock/version checks, policy evaluation, deployment insert, release transition, and audit event in one transaction | The decision and its evidence are atomic |
| Complete deployment | Deployment result, installed-version upsert, release transition, and audit event in one transaction | Dashboard state cannot get ahead of deployment history |
| Policy update | Expected optimistic-lock version, normalized policy values, and audit event in one transaction | Concurrent editors receive a conflict instead of overwriting one another |

Database constraints provide the final consistency boundary. Unique partial indexes allow only one pending approval per release/environment and one active deployment per product/environment. JPA `@Version` columns detect concurrent modification of products, releases, policies, installed versions, and deployment records.

## Release state machine

`DISCOVERED` is imported but not deployable. A governed release becomes `AWAITING_APPROVAL` or `APPROVED`, then `SCHEDULED`, `DEPLOYED`, and eventually `SUPERSEDED`. `BLOCKED` can return only to `DISCOVERED` for an explicit policy reassessment. Failed scheduling returns the release to `APPROVED`; the failed deployment remains immutable history.

Invalid transitions raise `GovernanceRuleException` before persistence. Check constraints reject unknown stored states. This dual layer gives useful API errors without relying solely on application correctness.

## Policy evaluation order

Hard blocks are evaluated first: an explicitly blocked release, prerelease in production, and a target below the minimum supported version. Approval and maintenance-window rules follow. Emergency mode can bypass only approval, window, and intentional downgrade rules when the product policy permits it. Each bypass becomes recorded deployment evidence.

Evaluation is deterministic for the supplied policy, release, installed version, approval state, emergency flag, environment, and timestamp. The clock is injected, making boundary tests reproducible.

## Source outage fallback

A failed fetch never deletes or supersedes known releases. The product records `FAILED` or `RATE_LIMITED`, a sanitized reason, and optional `nextCheckAfter`. The scheduler excludes a product until that timestamp. Operators continue to see last-known releases and may add a manual release after verifying it through an independent trusted channel.

Retries occur only for I/O failures, timeouts, and HTTP 502/503/504. HTTP 429 and GitHub's exhausted-rate-limit 403 are not aggressively retried; their reset information controls the next check. Permanent 4xx responses fail immediately. This prevents retry storms and distinguishes stale data from an empty release catalog.

## Audit guarantees

Application workflows append an audit row within the same transaction as the governed change. PostgreSQL triggers reject update and delete operations on `audit_events`. Details are JSON and deliberately omit credentials and response bodies. The model is tamper-resistant against ordinary application writes, but it is not cryptographically tamper-evident against a database administrator. External immutable log export would be the next production step.

## Deployment trade-offs

The service records intent and outcome rather than controlling infrastructure. This avoids storing target credentials and keeps the portfolio scope focused. It also means a caller could report an inaccurate result; production integration should authenticate deployment agents and accept signed callbacks or consume trusted pipeline events.
