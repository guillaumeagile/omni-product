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
- use branded or opaque types for stable identifiers and validated scalar concepts
- promote a branded primitive to a value object when behavior or richer invariants appear
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
- branded or opaque scalar types where primitive confusion would create domain bugs
- constructor/factory entry points
- aggregate methods that protect invariants
- narrow domain language in names and signatures

## Done when

- invalid combinations cannot be created accidentally
- same-shaped values with different domain meaning cannot be mixed accidentally
- domain rules live with the model, not scattered across services
- the model can be tested in memory with no framework bootstrapping
