# ADR-0007: Persist Idempotency Keys With Request Hashes

## Status

Accepted

## Context

Transaction creation must tolerate client retries and duplicate submissions.

## Decision

Persist idempotency keys by scope, key, and request hash. Replayed matching requests return the original response; mismatched requests are rejected.

## Consequences

The API has deterministic retry behavior. Storage cleanup can be handled later with expiry-based maintenance.
