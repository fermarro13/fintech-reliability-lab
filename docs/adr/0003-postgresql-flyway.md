# ADR-0003: Use PostgreSQL With Flyway Migrations

## Status

Accepted

## Context

Reliability patterns such as idempotency, outbox, audit, and ledger posting need durable transactional storage.

## Decision

PostgreSQL is the source of truth and Flyway owns all schema migrations.

## Consequences

Schema changes are reviewable, deterministic, and compatible with Testcontainers-based integration tests.
