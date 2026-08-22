# Agent Stack Routing

## Purpose

Use this stack to keep agent behavior precise, composable, and low-overhead.

## Global rules

- Read `agents/rules/CUTE and CUPID Code Rules.md` first for DDD/architecture posture (bounded contexts, testability,
  invariants), then `agents/rules/typescript-style.md` for TypeScript type-system and FP mechanics (unions, Result,
  mapped types). Apply both as shared posture for all tasks.
- follow `docs/5.the FP fellowship.md` for writing clean code.
- Pick **one primary skill** per task.
- Add a secondary skill only when the task clearly crosses concerns.
- Prefer **TypeScript-native** design before introducing libraries (except `neverthrow` for `Result<T, E>`).
- Keep **NestJS, Prisma, and HTTP** at the edges.
- Follow `../CLAUDE.md`

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

### `tdd-by-the-book`

Owns:

- the red/green/refactor loop itself when a task is explicitly done "using TDD"
- sequencing: one failing test, confirmed red, before any implementation
- keeping implementation steps minimal and test-driven

Does not own:

- domain modeling decisions (defer to `tactical-ddd-always-valid`)
- error algebra shape (defer to `result-type-functional-errors`)
- test technique/PBT design (defer to `domain-unit-testing-and-pbt`)

## Recommended order

1. `nest-cupid-architecture`
2. `tactical-ddd-always-valid`
3. `result-type-functional-errors`
4. `domain-unit-testing-and-pbt`
5. `mutation-testing-verification`

When a task is explicitly TDD-driven, `tdd-by-the-book` governs the *process*
(loop discipline) while the others still govern the *content* of each test and
each implementation step.

## Escalation rules

- If the task is about **shape of the model**, use `tactical-ddd-always-valid`.
- If the task is about **expected failure flow**, use `result-type-functional-errors`.
- If the task is about **confidence in behavior**, use `domain-unit-testing-and-pbt`.
- If the task is about **test rigor**, use `mutation-testing-verification`.
- If the task is about **layering, boundaries, or Nest wiring**, use `nest-cupid-architecture`.
- If the task explicitly says **"using TDD"**, apply `tdd-by-the-book` as the process on top of whichever content skill fits.
