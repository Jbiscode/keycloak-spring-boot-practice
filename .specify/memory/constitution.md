<!--
Sync Impact Report
- Version change: template -> 1.0.0
- Modified principles:
  - Principle slot 1 -> I. Learning Flow Drives Delivery
  - Principle slot 2 -> II. Reproducible Local Environment Is Mandatory
  - Principle slot 3 -> III. Auth Boundaries Must Be Explicit
  - Principle slot 4 -> IV. Every Story Requires Executable Verification
  - Principle slot 5 -> V. Keep Components Simple, Isolated, and Reusable
- Added sections:
  - Security and Environment Constraints
  - Implementation Workflow
- Removed sections:
  - None
- Templates requiring updates:
  - ✅ /Users/sajaebin/Documents/dev/keycloak_prac/keycloak_v1/.specify/templates/plan-template.md
  - ✅ /Users/sajaebin/Documents/dev/keycloak_prac/keycloak_v1/.specify/templates/spec-template.md
  - ✅ /Users/sajaebin/Documents/dev/keycloak_prac/keycloak_v1/.specify/templates/tasks-template.md
  - ✅ /Users/sajaebin/Documents/dev/keycloak_prac/keycloak_v1/AGENTS.md
  - ✅ `.specify/templates/commands/*.md` not present, no update required
- Follow-up TODOs:
  - Generate /Users/sajaebin/Documents/dev/keycloak_prac/keycloak_v1/specs/001-keycloak-setup/tasks.md from the updated workflow
-->
# Keycloak IAM Practice Constitution

## Core Principles

### I. Learning Flow Drives Delivery
This repository MUST implement features in the order learners experience them:
environment boot, token issuance, protected API access, then role-based access
control. A spec or plan MUST identify the P1 learning path first and prove that
path works before lower-priority extensions are added. Rationale: this project is
for hands-on learning, so delivery order must match how a beginner verifies
understanding.

### II. Reproducible Local Environment Is Mandatory
All runnable features MUST be reproducible from repository state with Docker
Compose, checked-in configuration, and scripted commands. Keycloak realms,
clients, roles, and demo users MUST be provisioned from versioned files rather
than manual console setup. Rationale: a broken or manual setup destroys the main
value of a practice repository.

### III. Auth Boundaries Must Be Explicit
Every protected route MUST declare whether it is public, authenticated, or bound
to a specific role. Keycloak-issued role claims MUST be mapped explicitly into
Spring Security authorities, and hostnames used for issuer validation MUST be
documented for both host and container contexts. Rationale: this repository
exists to teach authentication and authorization, so hidden security behavior is
not acceptable.

### IV. Every Story Requires Executable Verification
Each user story MUST include at least one executable verification path using
repository commands such as `docker compose`, `./gradlew test`, or `curl`.
Automated tests SHOULD cover mapping, authorization, and controller behavior;
when automation is impractical, a manual verification procedure MUST be written
in the spec or quickstart. Rationale: learners need a reliable way to confirm
401, 403, 200, and JWT claim behavior without guessing.

### V. Keep Components Simple, Isolated, and Reusable
Security mapping, controllers, DTOs, and service logic MUST remain narrowly
scoped and easy to copy into another sample service. Shared constants and error
handling MUST be centralized when they reduce repeated security logic, and new
abstractions MUST be justified by repeated use. Rationale: this project is a
teaching reference, not a framework experiment.

## Security and Environment Constraints

- Runtime baseline MUST remain Keycloak 24.x, Java 17, Spring Boot 3.2.x,
  PostgreSQL 15, and Docker Compose unless a spec explicitly records a migration.
- Secrets in examples MUST use `.env` or documented local overrides and MUST NOT
  be hard-coded in Java source.
- `issuer-uri`, `jwk-set-uri`, and Docker service names MUST be documented when
  host and container network contexts differ.
- Public endpoints MUST be limited to health-check or learning-safe routes.
  Debug or token-inspection endpoints MUST still require authentication unless a
  spec explicitly justifies otherwise.

## Implementation Workflow

1. Write or update the feature spec with prioritized user stories and explicit
   verification scenarios.
2. Produce a plan that passes the Constitution Check before implementation
   starts.
3. Build the runnable environment first: Docker services, realm import, and
   startup health.
4. Implement the P1 path end-to-end and verify it with executable commands.
5. Add higher-priority protected routes and RBAC checks one story at a time,
   re-running automated and manual verification after each story.
6. Update learner-facing run instructions when commands, ports, credentials, or
   verification steps change.

## Governance

This constitution overrides conflicting project habits and templates. Every plan,
spec, task list, and review MUST check compliance with these principles.
Amendments MUST include: the proposed rule change, impacted templates or docs,
and a semantic version update justified as MAJOR for incompatible governance
changes, MINOR for new principles or materially expanded requirements, and PATCH
for clarifications only. Compliance review MUST happen during planning and again
before merge for any change that affects environment setup, authentication,
authorization, or learner verification flow.

**Version**: 1.0.0 | **Ratified**: 2026-03-23 | **Last Amended**: 2026-03-23
