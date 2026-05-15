---
name: project-loomi-challenge
description: Loomi technical challenge - Order Processing System context and decisions
metadata: 
  node_type: memory
  type: project
  originSessionId: 3110e6f1-6465-424b-9303-fd7c25dbfca2
---

Order Processing System for Loomi Java technical challenge.

**Why:** Job selection process at Loomi, deadline 2026-05-15 at 12:00.

**GitHub:** https://github.com/M4teusPrado/loomi-order-processing

**Stack:** Java 21, Spring Boot 3.3.5, PostgreSQL (prod), H2 (local), Apache Kafka + Zookeeper, Flyway, Testcontainers, Maven, Docker Compose.

**Architecture:** MVC layered (Controller → Service → Repository), Strategy pattern per order type (5 types: PHYSICAL, SUBSCRIPTION, DIGITAL, PRE_ORDER, CORPORATE). Each item processed by its own processor independently.

**Profiles:**
- `application.yml` (default) → H2 in-memory, no Kafka, no Flyway — `./mvnw spring-boot:run`
- `application-dev.yml` (dev) → PostgreSQL + Kafka + Flyway — `-Dspring-boot.run.profiles=dev`

**Current state:** Skeleton pushed — model layer only (Order, OrderItem, Product, enums, MetadataConverter, migrations, Dockerfile, docker-compose, Makefile).

**How to apply:** Next steps follow the implementation plan at `docs/superpowers/plans/2026-05-13-order-processing.md`. Implement layer by layer with PRs per feature using conventional commits.
