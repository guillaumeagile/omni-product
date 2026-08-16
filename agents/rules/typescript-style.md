# Modern TypeScript Style Guide

## Purpose

Teach the agent to write modern, elegant TypeScript that is explicit, predictable, domain-readable, and pleasant to
evolve.

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

- **Use generics to preserve meaning, not to generalize prematurely.**
  A generic is justified when it keeps a real relationship explicit across inputs and outputs.

- **Derive types from one source of truth.**
  Use mapped and conditional types to avoid repeating structural knowledge in many places.

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
- Prefer **readonly** data and immutable updates.
- Prefer **typed IDs, value objects, and narrow scalar types** over raw strings and numbers when values carry domain
  meaning.
- Prefer **constructors or factory functions** that create trusted values only after validation.
- Prefer **tagged error unions** over broad exception hierarchies for expected business failures.
- Prefer type inference inside implementations, but annotate **public APIs, exported functions, and boundaries**.
- Prefer **explicit mapping** between DTOs, persistence rows, commands, and domain objects.
- Prefer **small local generic helpers** over early abstraction into reusable frameworks.
- Prefer exhaustive `switch` handling so new cases fail loudly at compile time.
- Prefer named derived types over repeating opaque inline type expressions.

## Done when

- the code reads in business language
- important guarantees are visible in types and signatures
- invalid states are rejected early or made impossible to express
- trusted data is clearly separated from raw external input
- the TypeScript feels modern, simple, and natural rather than clever
