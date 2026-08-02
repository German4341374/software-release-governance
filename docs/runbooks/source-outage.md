# Runbook: version source unavailable

## Symptoms

- Product source status is `FAILED` or `RATE_LIMITED`.
- `lastCheckError` contains a sanitized timeout, response status, or validation error.
- Newly published releases do not appear, while previously imported releases remain visible.

## Impact

Source discovery is stale. Existing approvals, policies, deployments, installed versions, and last-known releases remain usable. Do not interpret an unavailable source as proof that no update exists.

## Diagnose

1. Open the product page or call `GET /api/products/{id}` and record `lastCheckedAt`, `lastCheckStatus`, and `nextCheckAfter`.
2. Correlate structured logs using the request correlation ID. Do not paste tokens or complete third-party responses into tickets.
3. Verify `sourceReference`. GitHub must be `owner/repository`; static JSON must be an HTTPS URL resolving to a public address.
4. For GitHub `RATE_LIMITED`, inspect `nextCheckAfter`. Confirm the optional token is present in the runtime environment without printing its value.
5. From an approved diagnostic host, test DNS and HTTPS connectivity to the exact origin. Do not disable URL validation or switch to HTTP.
6. If the source returns JSON, validate the documented adapter contract and semantic version tags.

## Recover

1. Correct the product source or upstream response.
2. Wait for the scheduled retry or invoke `POST /api/products/{id}/refresh` once. Avoid repeated manual refreshes during an upstream outage.
3. Confirm `lastCheckStatus` becomes `SUCCESS`, `lastCheckError` clears, and expected releases appear.
4. If an urgent version must be governed, independently verify its version and release notes, import it manually with a unique external ID, and document the evidence in the notes. Manual import is a fallback, not a way to evade policy.

## Escalate

Escalate when multiple sources fail, DNS or certificate validation fails, the database cannot store check results, or stale discovery could breach a support deadline. Include timestamps, source types, safe status codes, correlation IDs, and affected product IDs—never secrets.

## Post-incident checks

- Confirm scheduled checks resume and do not create duplicate releases.
- Review audit events for manual imports and emergency deployments during the outage.
- Rotate a token only if exposure is suspected; an ordinary rate limit does not require rotation.
- Capture upstream availability and timeout evidence for tuning, without increasing retries blindly.
