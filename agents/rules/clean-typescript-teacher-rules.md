# Code Rules

## Purpose

Teach the agent to write modern, elegant TypeScript with clean code tactics borrowed from the workshop principles.

## Core posture

- Start from the **domain meaning**, not from the database shape.
- Use **business language** in names, modules, and types.
- Make the **important rules explicit** in types, constructors, and transitions.
- Prefer **small composable parts** over large flexible containers.
- Write code that is **predictable to read, predict, and test**.

## Teach these CUTE rules

### Be contextual

- Structure code by **bounded context** and business capability.
- Split concepts that mean different things in different contexts.
- Do not force one global model where several local models are clearer.

### Be ubiquitous

- Use the **domain vocabulary** in type names, method names, and modules.
- Replace technical placeholders with business concepts.
- Keep one meaning per name.

### Be testable

- Design pure domain logic so it can run **in memory**.
- Isolate framework and persistence concerns from the core model.
- Make dependencies explicit and easy to substitute.
- Write tests and assertions so humans can read them in **domain language**.
- Treat tests as **living documentation** of business rules and expected behavior.

## Testability expectations

- Expect **most business rules** to be testable without Nest bootstrapping.
- Expect **core domain logic** to run without database access, network access, clocks, or hidden globals.
- Expect dependencies on time, randomness, persistence, and external systems to be **explicit seams**.
- Expect tests to verify **observable business behavior**, not internal implementation steps.
- Expect pure logic tests to be **fast, deterministic, and cheap to run repeatedly**.
- Expect boundary tests to stay thin and focus on mapping, wiring, and integration contracts.
- Expect failures to point to a **broken rule**, not fragile setup.
- Expect test names and fixtures to speak in **domain language**.
- Encourage creating a test DSL (domain specific language) to make them more readable and have reusable test patterns,
  like ObjectMother or TestDataBuilder.

### Be expressive

- Prefer intent-revealing names over short generic names.
- Let structure explain behavior before comments do.
- Expose meaningful operations, not generic data mutation.

## Teach these CUPID rules

### Be composable

- Build small focused types that combine cleanly.
- Keep interfaces narrow.
- Prefer explicit composition over hidden coupling.

### Follow Unix philosophy

- Let each module do **one thing well**.
- Do not let services accumulate unrelated responsibilities.
- Split read models, write models, policies, and integrations when their reasons to change differ.

### Be predictable

- Keep invalid states out of the model.
- Separate **absence**, **expected business failure**, and **unexpected defect**.
- Keep side effects visible and late.
- Avoid surprising mutation and hidden control flow.

### Be idiomatic

- Use **TypeScript-native** tools first: unions, readonly data, mapped types, conditional types, utility types.
- Use NestJS for delivery and wiring, not as the domain model.
- Use Prisma as infrastructure, not as the source of business truth.

### Be domain-based

- Model behavior where the business rule lives.
- Co-locate data and behavior when they form one domain concept.
- Do not let DTOs, ORM rows, or transport schemas pretend to be the domain.

## Boundary rules

- Validate transport shape at the application boundary.
- Enforce business invariants inside the domain model.
- Map domain failures to HTTP responses only at the outer layer.
- Keep NestJS decorators, exceptions, and ORM types out of domain code.

## Modeling rules

- Eliminate primitive obsession when a value carries domain meaning.
- Use **value objects** for quantities, rates, identifiers, and constrained concepts.
- Use **aggregates** to protect invariants across state transitions.
- Publish **domain events** for significant domain changes.
- Keep automatic mapping and generic mutation away from core business logic.

## Clean code rules

- Choose names that reveal intent.
- Keep functions short and single-purpose.
- Make dependencies explicit.
- Remove duplicated domain logic by moving it into the right model.
- Do not hide business rules in controllers, mappers, or repository glue.
- Do not mirror the database structure unless the domain truly matches it.

## What to avoid

- god services
- god DTOs
- anemic domain models
- optional-field bags modeling several concepts at once
- ORM types leaking across the codebase
- control-flow exceptions for normal business outcomes
- auto-mapping that erases business intent
- technical names where domain names should exist

## Done when

- the code reads in business language
- the model rejects nonsense early
- pure logic is easy to test in isolation
- effects are pushed to the edge
- each file and module has one clear job
- the TypeScript feels modern, simple, and natural rather than clever
