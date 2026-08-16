# Instructor Action Plan: 2-Hour Tactical DDD & Quality Workshop

> **Target**: A focused 2-hour hands-on workshop transforming an anemic NestJS/Prisma God-model codebase into tactical
> DDD / CUPID domain models, verified by Property-Based Testing (PBT) and Mutation Testing.

---

## 🧭 Strategic Alignment & Core Decisions

1. **Shared Kernel / Pricing**: All shared pricing, tax, and margin rules live explicitly under `src/pricing/` (or
   shared kernel).
2. **Always-Valid Domain Models & Functional Result Types**: Domain factories and operations return
   `Result<T, DomainError>` / `Option<T>` instead of throwing control-flow exceptions.
3. **Quality Verification Hierarchy**:
    - Level 1: Isolated Unit Tests (Vitest in-memory, 0ms latency)
    - Level 2: Property-Based Testing (PBT with `fast-check` to verify mathematical & business invariants across
      arbitrary inputs)
    - Level 3: Mutation Testing (Stryker to kill 100% of arithmetic and boundary mutants)

---

## 🤖 Defined Agent Skills Stack

| Skill                               | Focus                      | Key Rules & Tactics                                                                                                                              |
|:------------------------------------|:---------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| **`tactical-ddd-always-valid`**     | Value Objects & Aggregates | Immutable VOs, private state in Aggregates, factory methods, zero primitive obsession, zero framework dependencies in domain layers.             |
| **`result-type-functional-errors`** | Monadic Error Handling     | `Result<T, DomainError>` / `Option<T>` for all fallible operations. Controllers map `Result.fail` to HTTP status codes (400, 404, 422).          |
| **`domain-unit-testing-and-pbt`**   | Unit & Invariant Testing   | Fast in-memory Vitest unit tests + Property-Based Tests (`fast-check`) asserting invariant symmetry, bounds, and idempotence.                    |
| **`mutation-testing-verification`** | Quality Verification Gate  | Run Stryker (`pnpm test:mutation`) to prove tests catch all boundary mutations and arithmetic shifts.                                            |
| **`nest-cupid-architecture`**       | Idiomatic NestJS & CUPID   | Strict 1-object-per-file (`../../CLAUDE.md`), Ports & Adapters (domain repository interfaces, Prisma infrastructure adapters), Bounded Contexts. |

---

## 📋 Execution Roadmap

- [x] **Task 0**: Agree on Agent Skills, Decisions, and Architecture.
- [ ] **Task 4 (Priority)**: Draft the complete 2-Hour Tactical Hands-On Guide in `2h-tactical-hands-on.md`.
- [ ] **Task 1**: Install `fast-check` and configure test dependencies.
- [ ] **Task 2**: Implement `src/shared/result.ts` (`Result<T, E>` and `DomainError`).
- [ ] **Task 3**: Build the reference tutorial in `src/pricing/`:
    - `Margin` Value Object (immutable, factory returning `Result<Margin, DomainError>`).
    - Unit Tests + Property-Based Tests + Stryker Mutation verification (100% killed).
- [ ] **Task 5**: Prepare candidate exercises and starter templates for participants (Pricing, Inventory, Catalog,
  Procurement tracks).
