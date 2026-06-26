# ADR-0001: Use A Modular Monolith For Version 1

## Status

Accepted

## Context

The project needs to demonstrate senior backend architecture without becoming a distributed-systems theater piece.

## Decision

Version 1 will be one Spring Boot deployable with explicit package boundaries for accounts, wallets, quotes, transactions, and platform reliability.

## Consequences

This keeps local development and testing fast while still making future service extraction possible if a bounded context earns that complexity.
