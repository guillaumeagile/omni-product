# Skill: result-type-functional-errors

## Use when

- a use case can fail in an expected business way
- absence, validation failure, and defects need different treatment
- exception-driven control flow is obscuring intent

## Goal

Model expected failure explicitly with `Result<T, E>` and `Option<T>`.

## Do

- use `Option<T>` for absence
- use `Result<T, DomainError>` for expected business failure
- use tagged union errors with a stable `kind`
- keep error payloads serializable and specific
- map domain errors to HTTP responses only at the boundary
- let unexpected technical failures throw

## Avoid

- using exceptions for ordinary business outcomes
- large class hierarchies for domain errors
- mixing transport errors with domain errors
- returning `null` where failure kind matters

## Deliverables

- a small local `Result` protocol
- domain-specific tagged error unions
- explicit boundary mapping rules
- signatures that expose expected failure modes

## Done when

- a caller can see possible business failures in the type signature
- error handling is exhaustive and predictable
- controllers translate errors without leaking HTTP concerns inward
