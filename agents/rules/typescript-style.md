# Modern TypeScript Style Guide

## Purpose

Teach the agent to use the TypeScript type system and FP-style constructs well: strong typing, algebraic data modeling,
`Result`-based error handling. This file is mechanics — for *why* (DDD/CUPID posture, bounded contexts, testability) see
`agents/rules/CUTE and CUPID Code Rules.md`.

**References** (read for full rationale — this file only distills them):

- `docs/4.the FP idiomatic way.md` — full FP patterns, Result design, library ladder
- `docs/5.the FP fellowship.md` — the FP habits (immutability, ADTs, smart constructors, …) mapped to tactical DDD
- Project `CLAUDE.md` — one type/interface/record per file convention, applies to every pattern below

## Objects for models, functions for actions

Domain models (value objects, entities) are classes with a private constructor and invariant-preserving methods — see
`agents/skills/tactical-ddd-always-valid.md`. Everything below in this file — `Result`/`Option` pipelines, `map`,
`andThen`, `match` — is about actions and orchestration (services, use cases), not about how a domain model's own
methods are written.

## Core posture

- Start from **domain meaning**, not from data shape alone.
- Use the type system to make important guarantees **explicit**.
- Prefer **small composable types** over large flexible containers.
- Write TypeScript that is easy to **read, predict, test, and refactor**.
- Use advanced type features to clarify the model, not to show cleverness.

## TypeScript Design Rules

- **Model relationships, not just shapes.**
  Use types to express how concepts relate, not only what fields they contain.

- **Encode guarantees at the type level when the guarantee is stable and important.**
  If a rule matters everywhere, make it visible in the model instead of rechecking it ad hoc.

- **Distinguish trusted domain data from untrusted external data.**
  Parsed, validated, and constructed domain values must not have the same status as raw input.

- **Use branded or opaque types when a primitive carries stable domain meaning.**
  A `string` that means `ProductId`, `Sku`, or `EmailAddress` should not be interchangeable with any other `string` once
  trusted.

- **Use generics to preserve meaning, not to generalize prematurely.**
  A generic is justified when it keeps a real relationship explicit across inputs and outputs.

- **Derive types from one source of truth.**
  Use mapped and conditional types to avoid repeating structural knowledge in many places.

- **Expose stable literals once, then reuse them.**
  Repeated discriminants, status names, event names, and error codes are a refactoring smell. Export one source of truth
  and derive the type-level usage from it instead of scattering raw strings.

- **Prefer explicit state models over optional-property bags.**
  When variants differ by meaning, use unions and explicit cases.

- **Model transitions, not only entities.**
  Types should help show what an operation requires and what it guarantees afterward.

- **Separate compile-time modeling from runtime validation.**
  Types describe trusted structures; runtime code must still decode and validate outside data.

- **Keep type-level design readable.**
  If a type trick makes the model harder to understand, simplify it.

- **Let types clarify the domain, not hide it.**
  The type system should reveal business intent, not replace good naming and good modeling.

## Practical TypeScript Defaults

- Prefer **discriminated unions** over booleans that smuggle state.
- Prefer **one exported source of truth** for repeated discriminants and other stable literals rather than repeating raw
  strings across constructors, guards, matches, and tests.
- Prefer **readonly** data and immutable updates.
- Prefer **branded or opaque types** for identifiers and validated scalar concepts when confusion between same-shaped
  values would create real domain bugs.
- Prefer **typed IDs, value objects, and narrow scalar types** over raw strings and numbers when values carry domain
  meaning.
- Prefer **`neverthrow` `Result` / `ResultAsync`** for expected business failures.
- Prefer a **small local `Option<T>`** for meaningful absence instead of adding a second FP dependency just for
  `Option`.
- Prefer **constructors or factory functions** that create trusted values only after validation.
- Prefer **tagged error unions** over broad exception hierarchies for expected business failures.
- Prefer type inference inside implementations, but annotate **public APIs, exported functions, and boundaries**.
- Prefer **explicit mapping** between DTOs, persistence rows, commands, and domain objects.
- Prefer **small local generic helpers** over early abstraction into reusable frameworks.
- Prefer exhaustive `switch` handling so new cases fail loudly at compile time.
- Prefer named derived types over repeating opaque inline type expressions.

## Patterns (worked shapes, not templates to copy verbatim)

State as a union, not booleans:

```ts
type ProductStatus =
        | { kind: 'Draft' }
        | { kind: 'Active'; activatedAt: Date }
        | { kind: 'Archived'; archivedAt: Date; reason: string };
```

Branded primitive for a stable domain identifier:

```ts
type Brand<T, TBrand extends string> = T & { readonly __brand: TBrand };
type ProductId = Brand<string, 'ProductId'>;
```

Expected failure as a tagged union, not an exception hierarchy:

```ts
type ReserveStockError =
        | { kind: 'ProductNotFound'; productId: ProductId }
        | { kind: 'InsufficientStock'; available: number; requested: number };
```

`neverthrow` as the chosen `Result` protocol:

```ts
import {err, ok, type Result, type ResultAsync} from 'neverthrow';
```

Use cases return `Result<T, DomainError>` or `ResultAsync<T, DomainError>`; unexpected or infrastructure failures still
throw.

Local `Option<T>` for absence without adding another library:

```ts
const OPTION_KIND = {
  Some: 'Some',
  None: 'None',
} as const;

type Option<T> =
        | { kind: typeof OPTION_KIND.Some; value: T }
        | { kind: typeof OPTION_KIND.None };
```

Derive, don't repeat (one source of truth):

```ts
type New<T> = Omit<T, 'id' | 'createdAt' | 'updatedAt'>;
type Persisted<T> = T & Readonly<{ id: string; createdAt: Date; updatedAt: Date }>;
```

Boundary vs domain: decode/validate shape at the edge (controller/pipe, e.g. Zod `safeParse`), enforce invariants via
smart constructors in the domain type. Type-level enforcement only — see `CUTE and CUPID Code Rules.md` for the
architectural placement of that boundary.

## Done when

- the code reads in business language
- important guarantees are visible in types and signatures
- invalid states are rejected early or made impossible to express
- trusted data is clearly separated from raw external input
- the TypeScript feels modern, simple, and natural rather than clever
