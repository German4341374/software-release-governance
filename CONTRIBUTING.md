# Contributing

## Workflow

1. Create a focused branch from `main`.
2. Add or update tests with the behavior.
3. Run `./mvnw verify` and `docker compose config`.
4. Keep migrations immutable after release; add a new numbered migration instead.
5. Open a pull request describing policy, transaction, security, and rollback effects.

Use Conventional Commits, for example:

- `feat(policy): enforce minimum supported version`
- `fix(adapter): honor GitHub rate limit reset`
- `test(api): cover idempotent manual import`
- `docs(runbook): add source outage recovery`

Do not commit credentials, real corporate data, generated build output, IDE metadata, or a local `.env` file. Use fictional examples and sanitize logs.

## Design expectations

- Keep domain policy deterministic and independent from controllers.
- Do not hold a transaction during network I/O.
- Add a unique constraint for any new idempotency promise.
- Record governed state changes in the audit trail in the same transaction.
- Reject concurrent edits instead of silently applying last-write-wins behavior.
