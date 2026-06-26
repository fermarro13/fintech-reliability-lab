# ADR-0008: Defer Auth, Redis, And Full OpenTelemetry

## Status

Accepted

## Context

The first version should prove transaction reliability before adding adjacent infrastructure.

## Decision

Do not add Keycloak, Redis, distributed locks, or a full OpenTelemetry collector in Phase 1.

## Consequences

The project stays focused. These pieces can be introduced later when a concrete use case exists.
