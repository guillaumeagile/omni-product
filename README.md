# omni-product-nest-js — DDD Workshop Seed

This is a **teaching seed, not production code**. It deliberately ships as an
anti-pattern starting point, ported from a legacy Spring Boot `Product` God
object (see `from-java-legacy/model/`). Attendees will refactor this during
the workshop toward a proper DDD structure (aggregates, value objects,
repositories, application services).

See [DEV-SETUP-AND-GUIDE.md](./DEV-SETUP-AND-GUIDE.md) to get running.

## Stack

- [NestJS](https://nestjs.com/) (TypeScript, strict mode)
- [Prisma](https://www.prisma.io/) ORM targeting Postgres
- [Vitest](https://vitest.dev/) as the test runner (not Jest)
- [pnpm](https://pnpm.io/) as the package manager

## What's deliberately wrong here

- **One God `Product` model** (`prisma/schema.prisma`) that couples catalog
  presentation (name, slug, images), stock, supplier/pricing, and warehouse
  location all onto a single row — mirroring the legacy Java entity.
- **Anemic `ProductService`** (`src/products/product.service.ts`) mixes
  Prisma persistence directly with business logic, including
  `calculateResellerPrice`, which tangles supplier base price, a regional
  margin, and VAT-on-margin into one method.
- **No repository, no ports/adapters, no domain/infra separation, no value
  objects** — the controller calls the service directly, and the service
  calls Prisma directly. These are exactly what attendees will introduce.
- **No CQRS, no command/query bus, no mediator** — intentionally flat.
- **E2E tests hit a real, unmanaged, shared Postgres** with no reset between
  runs (see `test/products.e2e-spec.ts`) — meant to be felt as slow/flaky
  compared to isolated unit tests around an extracted domain.

Use these as your refactoring targets during the workshop.

## Why e2e tests run against a real Postgres, not an in-memory DB

An in-memory or ephemeral substitute is possible, but each has a real cost:

- **`pg-mem`** — fast, no Docker, but doesn't fully support `Json` column
  queries, which this schema relies on (`images`, `suppliersRegions`).
- **Testcontainers** — full Postgres compatibility, but not actually
  in-memory: still needs Docker, plus ~1-2s startup per run.
- **SQLite** — fast and in-memory-capable, but a different engine than
  production (`provider = "postgresql"`), so behavior can diverge.

This seed keeps e2e tests on a real, shared Postgres instance instead, with
no reset between runs (see `test/products.e2e-spec.ts`).

It's slower and
more state-dependent than the unit tests under `pnpm test`, which mock
Prisma entirely — worth keeping in mind as you decide what a given test
actually needs to cover.

## What good tests look like

Regardless of the layer, a test worth keeping is usually:

- **Isolated** — doesn't depend on state left behind by another test, and
  doesn't leave state behind for the next one.
- **Repeatable** — gives the same result every run, on any machine, in any
  order.
- **Fast** — a slow test gets skipped or run less often, which defeats its
  purpose.
- **Self-checking** — passes or fails on its own; no one should have to read
  logs or query a database to know the outcome.
- **Focused on one behavior** — a failing test should point at what broke,
  not require debugging to find out.
- **Automated and autonomous** — runnable with a single command, without a
  human setting up state by hand first.
