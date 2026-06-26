# ADR-0002: Use Java 25, Spring Boot 4.1.0, And Gradle

## Status

Accepted

## Context

The portfolio should reflect a modern JVM backend stack and the project owner prefers Java 25, Spring Boot 4.1.0, and Gradle.

## Decision

The project targets Java 25, Spring Boot 4.1.0, and Gradle 9.x.

## Consequences

CI must provision JDK 25. Local builds require JDK 25 or a configured Gradle toolchain.
