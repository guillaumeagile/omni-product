# Instructor Action Plan: 2-Hour Tactical DDD & Quality Workshop

> **Target**: A focused 2-hour hands-on workshop transforming an anemic NestJS/Prisma God-model codebase into tactical
> DDD / CUPID domain models, verified by Property-Based Testing (PBT) and Mutation Testing.

---

## 🧭 Strategic Alignment & Core Decisions

1. **Shared Kernel / Pricing**: All shared pricing, tax, and margin rules live explicitly under `src/pricing/` (or
   shared kernel).
2. **Always-Valid Domain Models & Functional Result Types**: Domain factories and operations return
   `neverthrow` `Result<T, DomainError>` / `ResultAsync<T, DomainError>` for expected business failures and a local
   `Option<T>` for meaningful absence instead of throwing control-flow exceptions.
3. **Quality Verification Hierarchy**:
    - Level 1: Isolated Unit Tests (Vitest in-memory, 0ms latency)
    - Level 2: Property-Based Testing (PBT with `fast-check` to verify mathematical & business invariants across
      arbitrary inputs)
    - Level 3: Mutation Testing (Stryker to kill 100% of arithmetic and boundary mutants)

---

## 🤖 Defined Agent Skills Stack

See `agents/AGENTS.md` for the entry point and routing order.

| Skill                               | Focus                      | Key Rules & Tactics                                                                                                                                                                      |
|:------------------------------------|:---------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`tactical-ddd-always-valid`**     | Value Objects & Aggregates | Immutable VOs, branded primitives where useful, private state in Aggregates, factory methods, zero primitive obsession, zero framework dependencies in domain layers.                    |
| **`result-type-functional-errors`** | Monadic Error Handling     | `neverthrow` `Result<T, DomainError>` / `ResultAsync<T, DomainError>` for expected failures, local `Option<T>` for absence, tagged errors, boundary mapping at controllers.              |
| **`domain-unit-testing-and-pbt`**   | Unit & Invariant Testing   | Fast in-memory Vitest unit tests + Property-Based Tests (`fast-check`) asserting invariant symmetry, bounds, and idempotence, with domain-readable assertions and test DSLs when useful. |
| **`mutation-testing-verification`** | Quality Verification Gate  | Run Stryker (`pnpm test:mutation`) to prove tests catch all boundary mutations and arithmetic shifts.                                                                                    |
| **`nest-cupid-architecture`**       | Idiomatic NestJS & CUPID   | Strict 1-object-per-file (`../../CLAUDE.md`), Ports & Adapters (domain repository interfaces, Prisma infrastructure adapters), Bounded Contexts.                                         |

---

## 📋 Execution Roadmap

- [x] **Task 0**: Agree on Agent Skills, Decisions, and Architecture.
- [x] **Task 4 (Priority)**: Draft the complete 2-Hour Tactical Hands-On Guide in `2h-tactical-hands-on.md`.
- [x] **Task 1**: Install `fast-check` and configure test dependencies.
- [x] **Task 2**: Implement `src/shared/option.ts` (`Option<T>` plus Result-shaped helpers such as `some`, `none`,
  `map`, `andThen`, `match`, and `unwrapOr`, fully tested).
- [ ] **Task 3**: Build the reference tutorial in `src/pricing/`:
    - `Margin` Value Object (immutable, branded where useful, factory returning `neverthrow`
      `Result<Margin, DomainError>`).
    - Domain-readable Unit Tests + Property-Based Tests + Stryker Mutation verification (100% killed).
- [ ] **Task 5**: Prepare candidate exercises and starter templates for participants (Pricing, Inventory, Catalog,
  Procurement tracks).
