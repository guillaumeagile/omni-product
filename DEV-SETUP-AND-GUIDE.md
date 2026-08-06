# Dev setup & guide

## Prerequisites

- Node.js 20+
- [pnpm](https://pnpm.io/installation)
- Docker (for local Postgres)

## First-time setup

```bash
pnpm install
cp .env.example .env
pnpm db:up              # starts local Postgres via docker-compose
pnpm prisma:migrate     # creates the Product/Supplier tables
pnpm start:dev          # runs the Nest app with watch mode, on :3000
```

## Everyday scripts

| Script | What it does |
| --- | --- |
| `pnpm start:dev` | Run the app with watch mode |
| `pnpm build` | Compile to `dist/` |
| `pnpm test` | Run unit tests (Vitest, mocked Prisma — fast, no DB needed) |
| `pnpm test:watch` | Unit tests in watch mode |
| `pnpm test:e2e` | Run e2e tests — **requires a running, migrated Postgres** (see below) |
| `pnpm prisma:studio` | Browse the DB in Prisma Studio |
| `pnpm db:up` / `pnpm db:down` | Start/stop local Postgres via Docker |

## Running e2e tests

`pnpm test:e2e` boots the real `AppModule` and talks to whatever Postgres is
reachable at `DATABASE_URL` in `.env` — there is no test-container spin-up or
schema reset built in. Before running it:

```bash
pnpm db:up
pnpm prisma:migrate
pnpm test:e2e
```

If port `5432` is already taken by another local project, either stop that
container or point `DATABASE_URL` in `.env` at a different port and update
`docker-compose.yml`'s port mapping to match.

This coupling to a live, shared, unreset database is intentional for the
workshop — you'll notice it's slower and more state-dependent than the unit
tests in `pnpm test`. That's the point: it's one of the things worth fixing
once the domain logic is pulled out of `ProductService` and given proper
seams to test in isolation.

## Project layout

```
prisma/schema.prisma        God Product model + Supplier (see README)
src/app.module.ts           Root module
src/prisma.service.ts       Thin PrismaClient wrapper
src/products/
  products.module.ts
  products.controller.ts    Thin — delegates to ProductService
  product.service.ts        Anemic service: persistence + business rules mixed
  create-product-input.ts   One interface per file (see CLAUDE.md)
  supplier-region-info.ts
from-java-legacy/model/     Original Spring Boot entities this was ported from
```

## Conventions

See [CLAUDE.md](./CLAUDE.md) for the one-object-per-file rule and other
project conventions.
