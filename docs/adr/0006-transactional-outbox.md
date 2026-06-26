# ADR-0006: Use The Transactional Outbox Pattern

## Status

Accepted

## Context

Writing database state and publishing an event directly to a broker creates a dual-write failure mode.

## Decision

Domain changes and outbox rows are committed in the same PostgreSQL transaction. A relay publishes outbox rows asynchronously.

## Consequences

Event publication becomes at-least-once. Consumers must be idempotent.
