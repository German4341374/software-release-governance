# Repository working agreement

- Keep source code, comments, documentation, examples, and commit messages in English.
- Use Java 25 language features only when they make behavior clearer.
- Preserve controller, service, domain/policy, adapter, and repository boundaries.
- Keep external I/O outside database transactions.
- Add Flyway migrations for schema changes; never rewrite an applied migration.
- Add tests for state transitions, policy changes, adapters, and persistence constraints.
- Never commit `.env`, tokens, passwords, real organization names, or production URLs.
- Run `./mvnw verify` before proposing a change. Run Docker checks when Docker is available.
- Use Conventional Commits and do not add attribution trailers unless a contributor explicitly requests them.
