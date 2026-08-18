# Skill: tactical-ddd-always-valid

## Use when

- designing or refactoring domain models
- replacing god objects with explicit concepts
- enforcing invariants at construction and transition time

## Goal

Make illegal states unrepresentable, or at least explicit and impossible to ignore.

## Objects for models, functions for actions

Domain models — value objects and entities — are **classes**: a private constructor, a static factory returning
`Result<T, DomainError>`, and instance methods that are the only way to operate on the value while preserving its
invariants. The private constructor is what makes a model *unforgeable* — nothing outside the class can produce an
instance that skipped validation.

Actions, commands, and orchestration — services, use cases, anything that acts *on* domain models rather than *being*
one — stay function-first: pure functions and `Result`/`Option` pipelines (`map`, `andThen`, `match`), per
`agents/rules/typescript-style.md`.

Algebraic infrastructure that isn't itself a domain concept (`Option<T>`, `Result<T, E>` and their combinators) is its
own category: function/ADT style, one cohesive module, per the exception in the project `CLAUDE.md`. It is vocabulary
the domain models are built with, not a domain model itself.

## Do

- model domain concepts with small named types
- implement value objects and entities as classes: private constructor, static factory returning `Result`,
  invariant-preserving instance methods
- use branded or opaque types for stable identifiers and validated scalar concepts
- promote a branded primitive to a value object when behavior or richer invariants appear
- use discriminated unions for legal variants and lifecycle states
- keep domain logic free of NestJS, Prisma, and transport concerns
- make state transitions explicit and intention-revealing
- write services, use cases, and other orchestration as pure functions operating on domain model instances

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
