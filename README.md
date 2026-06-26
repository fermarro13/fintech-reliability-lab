# Fintech Reliability Lab

Production-style backend lab for a fictional fintech transaction platform. The project focuses on reliability, idempotency, transactional consistency, event-driven processing, observability, and architecture documentation.

This repository is intentionally public-safe: it contains no proprietary workflows, real payment rails, real customer data, KYC/AML logic, or employer-specific implementation details.

## Phase 1 Foundation

Implemented foundation pieces:

- Java 25 and Spring Boot 4.1.0 Gradle project.
- Spring MVC, Actuator, JDBC, Flyway, Kafka client, Micrometer, Prometheus registry.
- Structured JSON logging configured through Spring Boot.
- Correlation ID filter using `X-Correlation-Id`.
- RFC 9457-style API problem details.
- Platform database migration for idempotency keys, outbox events, processed-event dedupe, and audit log.
- Docker Compose stack for PostgreSQL, Redpanda, Redpanda Console, Prometheus, and Grafana.
- Static OpenAPI seed at `docs/openapi/openapi.yaml`.
- Starter architecture overview and ADRs.
- GitHub Actions CI.

## Requirements

- JDK 25.
- Docker Desktop.
- PowerShell, Bash, or another shell capable of running Gradle and Docker commands.

The current machine default may be Java 17; this project intentionally targets Java 25. Gradle toolchains can provision Java 25 in supported environments. If that is unavailable, install JDK 25 locally before running the build.

## Local Infrastructure

Start dependencies:

```powershell
docker compose up -d postgres redpanda redpanda-console prometheus grafana
```

Local services:

- API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`, database/user/password `fintech_lab`
- Redpanda Kafka API: `localhost:19092`
- Redpanda Console: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`, username/password `admin`/`admin`

Run the app:

```powershell
.\gradlew.bat bootRun
```

Run tests:

```powershell
.\gradlew.bat test
```

## API

The first Phase 1 endpoint is:

```http
GET /api/v1/system/status
```

Operational endpoints:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

The OpenAPI contract starts at `docs/openapi/openapi.yaml` and will expand as Phase 2 domain endpoints are implemented.

## Architecture Documentation

- `docs/architecture/overview.md`
- `docs/adr/0001-modular-monolith.md`
- `docs/adr/0002-java-spring-gradle.md`
- `docs/adr/0003-postgresql-flyway.md`
- `docs/adr/0004-explicit-sql-over-jpa.md`
- `docs/adr/0005-redpanda-for-local-streaming.md`
- `docs/adr/0006-transactional-outbox.md`
- `docs/adr/0007-persisted-idempotency-keys.md`
- `docs/adr/0008-defer-optional-complexity.md`

## Next Milestone

Phase 2 will add the core domain and APIs for users, accounts, wallets, quotes, and transaction creation.
