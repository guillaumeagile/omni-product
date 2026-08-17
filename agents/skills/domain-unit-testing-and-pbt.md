# Skill: domain-unit-testing-and-pbt

## Use when

- verifying domain rules and pure use-case logic
- asserting invariants across many inputs
- preventing regressions in arithmetic, pricing, stock, or state transitions

## Goal

Prove behavior with fast unit tests first, then widen confidence with property-based tests.

## Do

- keep tests in memory and deterministic
- test domain functions through public behavior
- write example-based tests for named business scenarios
- add property-based tests for invariants, symmetry, bounds, and idempotence
- generate meaningful arbitrary data near boundaries
- write assertions so humans can read them in domain language
- treat tests as living documentation of business rules
- use a small test DSL or builders when they improve readability and reuse
- prefer precise assertions over broad snapshots

## Avoid

- booting Nest or the database for pure domain tests
- duplicating implementation logic in tests
- property tests with weak or trivial properties
- mixing transport concerns into domain test suites

## Deliverables

- fast unit tests for core rules
- property-based tests for invariants
- readable generators and helper fixtures
- tests that explain business intent in domain language

## Done when

- core domain behavior is covered by isolated tests
- invariants hold across arbitrary inputs
- test names and assertions read like business rules
- failures point to a business rule, not test setup noise
