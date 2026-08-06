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

It's possible to run e2e tests against an in-memory or ephemeral substitute
instead of a real, shared Postgres instance, but each option trades away
something relevant to this workshop:

- **`pg-mem`** — an in-memory JS reimplementation of Postgres. Fast, no
  Docker needed, but it doesn't support the full SQL surface. This schema
  uses `Json` columns (`images`, `suppliersRegions`), and Prisma's generated
  queries against JSON operators frequently hit unsupported features or
  subtly different behavior than real Postgres.
- **Testcontainers** — spins up a real, throwaway Postgres in Docker per
  test run. Full compatibility, but not actually in-memory: startup takes
  ~1-2s per container and it still depends on Docker, the exact dependency
  an in-memory swap is usually trying to avoid.
- **SQLite** (via Prisma's SQLite provider) — fast and in-memory-capable,
  but a different datasource engine than production. `Json` columns,
  migrations, and connection strings all diverge from what the schema
  declares (`provider = "postgresql"`), so you'd be testing against a
  different engine than the one this workshop is teaching.

More generally, swapping in a fake or lighter-weight engine for e2e tests
risks masking real Postgres-specific bugs (JSON query behavior, unique
constraint quirks, case sensitivity) that only show up against the real
thing — and it papers over the exact pain point this seed's e2e setup is
meant to surface: coupling tests to a live, unmanaged, shared database is
slow and stateful, which is part of why teams eventually pull business logic
out into a domain layer that can be tested in isolation, without a database
at all. Keeping e2e tests on real Postgres lets attendees feel that friction
firsthand rather than have it hidden from them.
