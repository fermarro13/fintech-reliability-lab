# ADR-0004: Prefer Explicit SQL-Oriented Persistence Over JPA

## Status

Accepted

## Context

The critical paths are transaction state changes, ledger writes, idempotency, and outbox claiming.

## Decision

Use Spring JDBC or Spring Data JDBC for explicit transaction and query control instead of starting with JPA.

## Consequences

The code will be more explicit and less magic-heavy, which fits the reliability focus. It may require more repository code than JPA.
