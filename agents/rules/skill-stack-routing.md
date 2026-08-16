# Agent Skill Stack Routing

## Purpose

Use this stack to keep agent behavior precise, composable, and low-overhead.

## Global rules

- Read `agents/rules/clean-typescript-teacher-rules.md` first and apply it as shared posture for all tasks.
- Pick **one primary skill** per task.
- Add a secondary skill only when the task clearly crosses concerns.
- Prefer **TypeScript-native** design before introducing libraries.
- Keep **NestJS, Prisma, and HTTP** at the edges.
- Follow `CLAUDE.md`: **one object/interface/record-like type per file**.

## Skill boundaries

### `tactical-ddd-always-valid`

Owns:

- value objects
- aggregates
- entity state transitions
- domain invariants
- illegal-state-unrepresentable modeling

Does not own:

- HTTP mapping
- repository adapters
- test strategy
- mutation analysis

### `result-type-functional-errors`

Owns:

- `Result<T, E>` / `Option<T>` conventions
- tagged `DomainError` unions
- expected failure modeling
- mapping business failure to boundary responses

Does not own:

- aggregate design
- folder/module structure
- testing strategy beyond small examples

### `domain-unit-testing-and-pbt`

Owns:

- unit tests for pure domain logic
- property-based tests for invariants
- generators and boundary coverage
- algebraic and business properties

Does not own:

- production architecture
- mutation tooling configuration
- HTTP controller tests unless needed for boundary mapping only

### `mutation-testing-verification`

Owns:

- mutation test runs
- surviving mutant analysis
- targeted test strengthening
- arithmetic and boundary mutation gates

Does not own:

- primary domain design
- repository/module structure
- replacing mutation failures with broad snapshot tests

### `nest-cupid-architecture`

Owns:

- bounded context structure
- ports and adapters
- Nest modules and dependency wiring
- DTO-to-command and persistence mapping boundaries
- framework isolation from domain code

Does not own:

- domain invariant details
- error algebra design
- test quality gates

## Recommended order

1. `nest-cupid-architecture`
2. `tactical-ddd-always-valid`
3. `result-type-functional-errors`
4. `domain-unit-testing-and-pbt`
5. `mutation-testing-verification`

## Escalation rules

- If the task is about **shape of the model**, use `tactical-ddd-always-valid`.
- If the task is about **expected failure flow**, use `result-type-functional-errors`.
- If the task is about **confidence in behavior**, use `domain-unit-testing-and-pbt`.
- If the task is about **test rigor**, use `mutation-testing-verification`.
- If the task is about **layering, boundaries, or Nest wiring**, use `nest-cupid-architecture`.
