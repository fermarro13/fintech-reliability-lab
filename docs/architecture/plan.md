                                         # Fintech Reliability Lab Architecture Plan

## 1. Project Vision
Build a fictional fintech transaction backend that proves reliability engineering, not CRUD volume. It demonstrates idempotent APIs, asynchronous transaction processing, transactional outbox, durable retries, duplicate-event safety, ledger-style wallet accounting, observability, integration testing, and ADR-driven architecture.

It intentionally does not cover real payments, real banking integrations, PCI, KYC/AML, production auth, multi-region design, distributed transactions, or proprietary workflows. This keeps it public, safe, and portfolio-focused.

## 2. Domain Model
Bounded contexts: `Accounts`, `Wallets/Ledger`, `Rates/Quotes`, `Transactions`, and `Platform Reliability`.

Core aggregates:
- `User` and `Account`: fictional customer ownership and account status.
- `Wallet`: account-owned balance by currency, with available and reserved amounts.
- `LedgerEntry`: append-only debit/credit audit record.
- `Quote`: immutable rate snapshot with expiry.
- `Transaction`: stateful transfer request with attempts, quote reference, and failure metadata.

Transaction states: `ACCEPTED -> PROCESSING -> FUNDS_RESERVED -> COMPLETED`; retry path `PROCESSING|FUNDS_RESERVED -> RETRY_PENDING -> PROCESSING`; terminal states `REJECTED` for business failures and `FAILED` for exhausted technical failures.

Key invariants: money amounts are positive; wallet balances never go negative; quotes are immutable and expire; ledger entries are append-only; one idempotency key cannot produce two different transactions; event handlers must tolerate duplicate delivery.

## 3. System Architecture
Use a modular monolith: one Spring Boot service, one deployable, clear package boundaries. This is stronger for a portfolio than premature microservices.

Runtime stack:
- Java 25, Spring Boot 4.1.0, Gradle 9.x, PostgreSQL, Redpanda, Flyway, JUnit 5, Mockito, Testcontainers, Micrometer, Prometheus, Grafana.
- Spring Boot 4.1.0 is compatible with Java 25 and Gradle 8.14+/9.x per Spring’s system requirements; JDK 25 is GA per OpenJDK.
- Use Spring MVC, Spring JDBC/JdbcClient or Spring Data JDBC, Flyway, Actuator, Kafka client/Spring Kafka, and Spring Boot structured JSON logging.

Architecture layers: controllers accept REST commands, application services enforce use cases, domain objects enforce invariants/state transitions, infrastructure implements persistence/messaging/provider adapters.

## 4. Reliability Patterns
Idempotency: require `Idempotency-Key` on transaction creation. Store `scope`, `key`, `request_hash`, `status`, `resource_id`, HTTP status, and response body. Same key + same hash returns the original response; same key + different hash returns `409`.

Outbox: every state-changing transaction writes domain data and an outbox row in the same PostgreSQL transaction. A relay publishes pending rows to Redpanda and marks them published after broker acknowledgement.

Retries: processing attempts are persisted with attempt number, error code, next attempt time, and backoff. Retry scheduling is database-driven, not sleep-based.

Duplicate events: every consumer records `(consumer_name, event_id)` before side effects in the same transaction. Ledger writes also use unique constraints to prevent double posting.

Partial failures: if the app crashes after DB commit but before Kafka publish, the outbox relay recovers. If the worker crashes after consuming, Redpanda redelivery plus consumer dedupe recovers. If funds were reserved and final failure occurs, reservation release is part of terminal failure handling.

External dependency failures: use a simulated `SettlementProvider` interface with deterministic transient/permanent failure modes. Real HTTP integrations, circuit breakers, and provider reconciliation are deferred.

## 5. API Design
Initial endpoints:
- `POST /api/v1/users`, `GET /api/v1/users/{id}`
- `POST /api/v1/accounts`, `GET /api/v1/accounts/{id}`
- `POST /api/v1/wallets`, `GET /api/v1/wallets/{id}`, `GET /api/v1/accounts/{id}/wallets`
- `POST /api/v1/sandbox/wallets/{id}/fund` for fictional local funding only
- `GET /api/v1/rates`, `POST /api/v1/quotes`, `GET /api/v1/quotes/{id}`
- `POST /api/v1/transactions`, `GET /api/v1/transactions/{id}`, `GET /api/v1/accounts/{id}/transactions`
- `/actuator/health`, `/actuator/prometheus`, `/actuator/info`

API errors use `application/problem+json` with `code`, `message`, `correlationId`, and retryability where useful. Transaction creation returns `202 Accepted` with `transactionId`, current status, and status URL.

For OpenAPI: maintain `docs/openapi/openapi.yaml` as the source of truth and expose Swagger UI via Docker Compose. Springdoc can be reconsidered later because current springdoc docs list Spring Boot v3 support, not Boot 4.

## 6. Database Design
Tables:
- `users`, `accounts`
- `wallets`: `id`, `account_id`, `currency`, `available_balance`, `reserved_balance`, `version`, `status`, audit columns
- `ledger_entries`: `id`, `wallet_id`, `transaction_id`, `entry_type`, `direction`, `amount`, `currency`, `balance_after`, `metadata`, `created_at`
- `quotes`: rate snapshot, source/target currency, amounts, rate, expiry, status
- `transactions`: wallets, quote, source/target amounts, status, attempts, failure fields, version, idempotency reference, correlation ID
- `transaction_attempts`
- `idempotency_keys`
- `outbox_events`
- `processed_events`
- `audit_log`

Important constraints/indexes:
- Unique `(account_id, currency)` on wallets.
- Unique `(scope, idempotency_key)` and stored request hash.
- Unique ledger dedupe key such as `(transaction_id, wallet_id, entry_type)`.
- Index `transactions(status, next_attempt_at)`.
- Partial index for unpublished outbox rows by `status`, `next_attempt_at`, `created_at`.
- Primary key `(consumer_name, event_id)` on processed events.

## 7. Event Design
Event envelope: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `aggregateType`, `aggregateId`, `aggregateVersion`, `correlationId`, `causationId`, and `payload`.

Initial event names:
- `transaction.accepted.v1`
- `transaction.processing_requested.v1`
- `transaction.retry_scheduled.v1`
- `transaction.completed.v1`
- `transaction.rejected.v1`
- `transaction.failed.v1`
- `wallet.balance_changed.v1`

Producer responsibility: write events only through the outbox. Consumer responsibility: dedupe first, perform idempotent domain transition, write audit/ledger changes, then commit.

## 8. Observability Plan
Logs: JSON using Spring Boot structured logging, with `correlationId`, `transactionId`, `eventId`, `idempotencyKeyHash`, `attempt`, `status`, and error code.

Metrics: request latency/error rate, transaction status counts, processing duration, retry count, outbox backlog, publish failures, consumer lag, idempotency hits/conflicts, stuck transactions, wallet posting failures, DB pool, JVM.

Dashboards: transaction funnel, reliability health, outbox/consumer health, API health, JVM/Postgres/Redpanda basics.

Real production alerts to document: high failed transaction rate, outbox backlog growth, stuck processing states, retry storm, consumer lag, DB saturation, and elevated idempotency conflicts.

Tracing: defer full OpenTelemetry until logs and metrics are solid. Add Micrometer observations first; OTel exporter can be Phase 5 stretch.

## 9. Testing Strategy
Unit tests: money value objects, transaction state machine, idempotency request hashing, retry classification, ledger invariants.

Integration tests with Testcontainers: PostgreSQL migrations/repositories, Redpanda publish/consume flow, outbox relay, duplicate events, and recovery jobs.

API tests: controller validation, Problem Details responses, OpenAPI examples, idempotent transaction creation, quote expiry, insufficient funds.

Failure tests: duplicate POST, same key/different body, broker unavailable, crash-before-publish recovery, duplicate Kafka event, transient provider retry, exhausted retries, stuck transaction recovery.

## 10. Documentation Plan
README structure: project purpose, architecture summary, local quickstart, reliability scenarios, API examples, observability screenshots, test strategy, ADR index, and “What I would improve next.”

Docs to include: C4-style architecture diagram, transaction sequence diagram, outbox sequence diagram, local Docker Compose guide, OpenAPI spec, sample curl flows, failure-mode walkthroughs, and public-safety note that all data/funds are fictional.

## 11. Repository Structure
Use one Gradle project, package-by-feature:
- `src/main/java/.../accounts`
- `src/main/java/.../wallets`
- `src/main/java/.../quotes`
- `src/main/java/.../transactions`
- `src/main/java/.../platform/idempotency`
- `src/main/java/.../platform/outbox`
- `src/main/java/.../platform/audit`
- `src/main/java/.../platform/observability`
- `src/main/resources/db/migration`
- `docs/adr`, `docs/openapi`, `docs/architecture`
- `infra/prometheus`, `infra/grafana`
- `.github/workflows/ci.yml`

Avoid a multi-service repo until there is a real extraction reason.

## 12. Milestone Roadmap
Phase 1, Foundation: Gradle/Spring Boot app, Docker Compose, PostgreSQL, Redpanda, Flyway, Actuator, CI, structured logging, base error model.

Phase 2, Core domain and APIs: accounts, wallets, sandbox funding, quotes, transaction creation, ledger model, persisted idempotency for `POST /transactions`.

Phase 3, Async processing and outbox: outbox table/relay, Redpanda topics, transaction processor, event envelope, processed-event dedupe.

Phase 4, Reliability and failure handling: retries, transient/permanent provider simulation, stuck transaction recovery, reservation release, failure scenario tests.

Phase 5, Observability: metrics, Prometheus scrape config, Grafana dashboards, alert documentation, optional tracing exporter.

Phase 6, Portfolio polish: README, diagrams, ADRs, OpenAPI examples, demo script, screenshots, “next improvements.”

## 13. ADRs To Create
- ADR-001: Modular monolith over microservices.
- ADR-002: Java 25, Spring Boot 4.1.0, and Gradle.
- ADR-003: PostgreSQL as source of truth with Flyway migrations.
- ADR-004: JDBC/Data JDBC over JPA for explicit transaction and ledger control.
- ADR-005: Redpanda as local Kafka-compatible event broker.
- ADR-006: Transactional outbox over direct broker publishing.
- ADR-007: Persisted idempotency keys with request hashing.
- ADR-008: Deferring auth, Redis, and full OpenTelemetry from v1.

## 14. Risks, Tradeoffs, And Assumptions
Main risk: building too much infrastructure before the transaction lifecycle is convincing. The first implementation should prove one excellent reliability path end-to-end before adding breadth.

Intentional simplifications: fictional funds, no real payment rails, no auth in v1, no distributed transactions, no exactly-once messaging claim, no multi-region strategy, no real compliance scope.

Deferred unless later justified: Keycloak, Redis locks/cache, real external HTTP provider, reconciliation engine, OpenTelemetry collector, Kubernetes, performance/load testing beyond basic smoke checks.

Assumptions: Redpanda is the local broker; PostgreSQL is the only source of truth; one deployable is the right v1 architecture; OpenAPI starts as a checked-in contract; Spring Boot’s built-in structured logging is preferred.

Sources checked: [Spring Boot 4.1.0 system requirements](https://docs.spring.io/spring-boot/system-requirements.html), [OpenJDK JDK 25](https://openjdk.org/projects/jdk/25/), [Spring Boot structured logging](https://docs.spring.io/spring-boot/reference/features/logging.html), [springdoc-openapi docs](https://springdoc.org/).
