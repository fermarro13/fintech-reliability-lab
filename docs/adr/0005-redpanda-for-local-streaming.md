# ADR-0005: Use Redpanda For Local Kafka-Compatible Streaming

## Status

Accepted

## Context

The system needs event-driven processing, but local developer experience should stay lightweight.

## Decision

Use Redpanda in Docker Compose as the Kafka-compatible broker.

## Consequences

The application can use Kafka clients while avoiding ZooKeeper and heavier local broker setup.
