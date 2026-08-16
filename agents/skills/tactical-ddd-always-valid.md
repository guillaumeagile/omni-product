# Skill: tactical-ddd-always-valid

## Use when

- designing or refactoring domain models
- replacing god objects with explicit concepts
- enforcing invariants at construction and transition time

## Goal

Make illegal states unrepresentable, or at least explicit and impossible to ignore.

## Do

- model domain concepts with small named types
- prefer immutable value objects and readonly data
- use discriminated unions for legal variants and lifecycle states
- expose factory functions or named constructors for validation
- keep domain logic free of NestJS, Prisma, and transport concerns
- make state transitions explicit and intention-revealing

## Avoid

- primitive obsession
- boolean soup for lifecycle state
- optional-field bags representing multiple cases
- setters that bypass invariants
- framework exceptions inside domain code

## Deliverables

- explicit domain types
- constructor/factory entry points
- aggregate methods that protect invariants
- narrow domain language in names and signatures

## Done when

- invalid combinations cannot be created accidentally
- domain rules live with the model, not scattered across services
- the model can be tested in memory with no framework bootstrapping
