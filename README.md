# sast-dashboard

The read side of the SAST pipeline POC: `dashboard-api` (Spring Boot, Redis cache-aside) and an Angular page.

> Part of a SAST pipeline built from open-source parts: [samples](https://github.com/ImohitI/samples) (vulnerable code + ground truth) → [os-sast](https://github.com/ImohitI/os-sast) (scanner CLI) → [sast-platform](https://github.com/ImohitI/sast-platform) (ingest API, Kafka, consumer, Jenkins) → **sast-dashboard** (dashboard API + UI).  
> Design decisions: [DESIGN.md](https://github.com/ImohitI/sast-platform/blob/main/docs/DESIGN.md) · one scan followed end to end: [WALKTHROUGH.md](https://github.com/ImohitI/sast-platform/blob/main/docs/WALKTHROUGH.md)

```
[ui :4200] ─GET /api─► [dashboard-api :8082] ─► [Redis latest:{repoId}, TTL 60 s] ── miss ──► [Postgres, read-only user]
```

Postgres and Redis are the ones started by `sast-platform`. The consumer there writes the data and deletes `latest:{repoId}` after each new build.

## dashboard-api

| Endpoint | Returns |
|----------|---------|
| `GET /api/repos` | repo ids that have at least one scan |
| `GET /api/repos/{repoId}/latest` | latest build: commit, time, counts by severity, findings. **Cache-aside** through Redis, logs `cache HIT` / `cache MISS` |
| `GET /api/repos/{repoId}/builds` | the 20 newest builds |
| `GET /api/repos/{repoId}/builds/{buildId}/diff?base=` | `{ newFindings, fixedFindings }` matched by fingerprint; `base` defaults to the previous build |

Design points:

- **Redis is optional.** Every Redis call is wrapped: an error is logged and Postgres answers. Timeouts are 500 ms, so an outage costs speed, not a 60 s hang (E5).
- **Read-only DB user** `dashboard_ro`, created by `sast-platform/postgres/init`. The consumer owns the schema.
- **The cache key `latest:{repoId}`** must match `CacheKeys` in `sast-platform/contracts`. This is a cross-repo contract, kept as a copy with a comment on both sides.
- **The diff is only as good as the fingerprint** `sha256(cwe|file|line)`: moving a bug down one line shows it as fixed + new.

## Run

```bash
# sast-platform infra must be up (Postgres + Redis), with at least one scan stored
cd dashboard-api && mvn spring-boot:run          # :8082
cd ui && npm install && npm start                 # :4200

# or both in Docker, on sast-platform's network
docker compose up -d --build                      # ui on :4200
```

Tests: `cd dashboard-api && mvn test` (diff logic, cache HIT / MISS / Redis down).

## License

[Apache-2.0](LICENSE)
