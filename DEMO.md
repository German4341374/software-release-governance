# Five-minute demonstration

## Before the meeting

```bash
cp .env.example .env
# Replace the development database password.
docker compose up --build --detach --wait
curl --fail http://localhost:8080/health
```

Keep the home page and a terminal visible. The included data is fictional.

## Minute 0–1: frame the problem

Open <http://localhost:8080>. Explain that the service answers three operational questions: which environments are outdated, whether a release is eligible for production, and who approved or reported each change. Point to the drift dashboard and the three environments.

## Minute 1–2: show source abstraction

Open **Operations Portal**. Show the releases imported through the manual adapter and mention the interchangeable GitHub Releases and static JSON adapters. Explain that external HTTP is outside the database transaction, uses timeouts and bounded retries, and preserves last-known-good releases during an outage.

## Minute 2–3: demonstrate governance

Show policy values: prereleases forbidden in production, approval required, minimum `1.2.0`, blocked patterns, and the UTC maintenance window. Try the beta `2.0.0-beta.1` against Production and explain that emergency mode cannot bypass hard safety rules.

## Minute 3–4: approval and deployment history

Open **Approvals**, approve the pending `1.4.0` request, then schedule it for Production inside the configured window or choose the Development environment for a time-independent live demo. Mark the record successful and show the installed version/dashboard update.

## Minute 4–5: prove engineering quality

```bash
./mvnw test
curl --fail http://localhost:8080/api/dashboard
docker compose ps
```

Point out the state-machine tests, policy boundary tests, Testcontainers integration suite, Flyway constraints, append-only audit trigger, non-root read-only container, pinned CI actions, and no automatic image publication or infrastructure deployment.

Finish by naming the honest limitations: actors are not authenticated, deployments are recorded rather than executed, and multi-instance scheduling would need distributed locking.
