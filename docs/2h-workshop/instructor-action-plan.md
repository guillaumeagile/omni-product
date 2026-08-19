# Instructor Action Plan: 2-Hour Tactical DDD & Quality Workshop

> **Target**: A focused 2-hour hands-on workshop transforming an anemic NestJS/Prisma God-model codebase into tactical
> DDD / CUPID domain models, verified by Property-Based Testing (PBT) and Mutation Testing.

---

## 🧭 Strategic Alignment & Core Decisions

1. **Shared Kernel / Pricing**: All shared pricing, tax, and margin rules live explicitly under `src/shared/pricing/`
   (settled by Task 3 — `2h-tactical-hands-on.md`'s Part 1 heading still says `src/pricing/`, not yet reconciled).
2. **Always-Valid Domain Models & Functional Result Types**: Domain factories and operations return
   `neverthrow` `Result<T, DomainError>` / `ResultAsync<T, DomainError>` for expected business failures and a local
   `Option<T>` for meaningful absence instead of throwing control-flow exceptions.
3. **Quality Verification Hierarchy**:
    - Level 1: Isolated Unit Tests (Vitest in-memory, 0ms latency)
    - Level 2: Property-Based Testing (PBT with `fast-check` to verify mathematical & business invariants across
      arbitrary inputs)
    - Level 3: Mutation Testing (Stryker to kill 100% of arithmetic and boundary mutants — a stubborn "unkillable"
      mutant is usually a modeling gap, not a tooling limit; see `agents vs mutants.md`)

---

## 🤖 Defined Agent Skills Stack

See `agents/AGENTS.md` for the entry point and routing order.

| Skill                               | Focus                      | Key Rules & Tactics                                                                                                                                                                      |
|:------------------------------------|:---------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`tactical-ddd-always-valid`**     | Value Objects & Aggregates | Immutable VOs, branded primitives where useful, private state in Aggregates, factory methods, zero primitive obsession, zero framework dependencies in domain layers.                    |
| **`result-type-functional-errors`** | Monadic Error Handling     | `neverthrow` `Result<T, DomainError>` / `ResultAsync<T, DomainError>` for expected failures, local `Option<T>` for absence, tagged errors, boundary mapping at controllers.              |
| **`domain-unit-testing-and-pbt`**   | Unit & Invariant Testing   | Fast in-memory Vitest unit tests + Property-Based Tests (`fast-check`) asserting invariant symmetry, bounds, and idempotence, with domain-readable assertions and test DSLs when useful. |
| **`mutation-testing-verification`** | Quality Verification Gate  | Run Stryker (`pnpm test:mutation`) to prove tests catch boundary mutations and arithmetic shifts. A survivor that resists every test rewrite is a cue to check the *types* first — an over-wide parameter conflating two domain cases — before excusing the mutant (see `agents vs mutants.md`). |
| **`nest-cupid-architecture`**       | Idiomatic NestJS & CUPID   | Strict 1-object-per-file (`../../CLAUDE.md`), Ports & Adapters (domain repository interfaces, Prisma infrastructure adapters), Bounded Contexts.                                         |

---

## 📋 Execution Roadmap

- [x] **Task 0**: Agree on Agent Skills, Decisions, and Architecture.
- [x] **Task 4 (Priority)**: Draft the complete 2-Hour Tactical Hands-On Guide in `2h-tactical-hands-on.md`.
- [x] **Task 1**: Install `fast-check` and configure test dependencies.
- [x] **Task 2**: Implement `src/shared/option.ts` (`Option<T>` plus Result-shaped helpers such as `some`, `none`,
  `map`, `andThen`, `match`, and `unwrapOr`, fully tested).
- [x] **Task 3**: Build the reference tutorial in `src/shared/pricing/margins/`:
    - `Margin` Value Object — implemented as a **class**, not a plain-object/function module: private constructor,
      `static fromPercentage(): Result<Margin, MarginError>`, `static forRegion(region: string)`,
      `static default()`, instance methods `applyTo`/`equals`. This is the settled resolution of a real tension the
      workshop's own worked example and the earlier function/ADT-styled rules disagreed on — see the "objects for
      models, functions for actions" rule now in `agents/skills/tactical-ddd-always-valid.md` and
      `agents/rules/typescript-style.md`. The branded `Rate` type is private to the class, not exported — validity
      is enforced purely by construction.
    - refer to Part 1: Phase A — Primitive Obsession → Value Objects (Pricing Shared Kernel) in
      `2h-tactical-hands-on.md`. Note: that section's `Margin` demo imports a `../../shared/result` module that does
      not exist in this repo — a dead reference in the workshop doc, not fixed as part of this task.
    - Domain-readable Unit Tests + Property-Based Tests: 25 tests, `src/shared/pricing/margins/margin.spec.ts`.
    - Stryker Mutation verification: **set up from scratch** — `@stryker-mutator/core` +
      `@stryker-mutator/vitest-runner` were not installed, no `stryker.config.json` or `test:mutation` script
      existed (only a stale, orphaned `.stryker-tmp/` sandbox from an earlier ad hoc run, now removed). Both are now
      in place; `pnpm test:mutation` runs against `src/shared/pricing/**/*.ts`.
    - Mutation score: **100.00%, zero survivors, gate at `break: 100`.** Getting there took a false start worth
      reading in full: `Margin.forRegion(region?: string)` had one `ConditionalExpression` mutant on its
      `region === undefined` guard that survived four separate code rewrites, each treating it as a testing/tooling
      problem. The actual defect was in the *type*, not the test suite or the code around the branch — `region` was
      optional when it shouldn't have been, conflating "resolve a named region's rate" with "give me the default
      rate" into one signature. Splitting them into `forRegion(region: string)` and a separate `default()` factory
      made the branch structurally impossible, and the mutant had nothing left to survive as. Full account,
      including why a language-level "these two branches are semantically equivalent" explanation was the wrong
      diagnosis: `agents vs mutants.md`.
- [ ] **Task 5**: Prepare candidate exercises and starter templates for participants (Pricing, Inventory, Catalog,
  Procurement tracks).
    