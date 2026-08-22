# 2-Hour Tactical Hands-On Lab: CUPID DDD & Code Quality

> **Goal**: Refactor an anemic NestJS/Prisma God-model codebase into clean, expressive, always-valid Tactical DDD models
> and verify behavioral resilience using **Unit Testing**, **Property-Based Testing (PBT)**, and **Mutation Testing (
Stryker)**.

---

## ⏱️ Workshop Schedule (120 Minutes)

```
00:00 - 00:15 (15 min) | Part 0: As-Is Tour — Legacy Smells & The Mutation Baseline
00:15 - 00:50 (35 min) | Part 1: Phase A — Primitive Obsession → Value Objects (Pricing Shared Kernel)
00:50 - 01:40 (50 min) | Part 2: Phase B — Anemic CRUD → Rich Aggregates & Invariants (Choose a Track)
01:40 - 01:55 (15 min) | Part 3: Phase C — Verification Gate (Unit + PBT + Mutation Testing)
01:55 - 02:00 (05 min) | Part 4: Retrospective & CUPID Properties Checklist
```

---

## 🧭 Foundational Tactical Rules

All code written during this lab must adhere to three foundational rules:

1. **Always-Valid Models via Result Monad**:
    - Constructors are private or protected; creation goes through static factory methods returning
      `Result<T, DomainError>`.
    - No exceptions for expected business failures.
2. **Zero Framework Pollution in Domain**:
    - Domain files under `src/<context>/domain/` must have **zero imports** from `@nestjs/*`, `@prisma/client`, or HTTP
      libraries.
3. **One Object Per File (`../../CLAUDE.md`)**:
    - Each Value Object, Entity, Interface, or Error type lives in its own file named after the type.

---

## 🔍 Phase 0: As-Is Tour & Mutation Baseline (15 min)

### The Legacy Smells

Inspect `../../src/products/product.service.ts` and `../../prisma/schema.prisma`:

- **God Entity**: One database table mixing catalog, inventory, pricing, supplier, and warehouse attributes.
- **Primitive Obsession**: Raw numbers for prices, stock, margins, and taxes with no invariant validation.
- **Anemic Logic**: Margin tables and VAT calculations buried in service methods.

### The False Sense of Security

Run the tests and mutation test runner:

```bash
pnpm test
pnpm test:mutation
```

- **Observation**: Unit tests pass (100% green), yet **Stryker reports >50% survived mutants** (arithmetic changes,
  swapped `>=` / `>`, and missed validation boundaries).

---

## 💎 Phase A — Value Objects in Shared Kernel (`src/pricing/`) (35 min)

### 1. Instructor Live Demo: `Margin` Value Object (10 min)

We extract the hardcoded margin `0.18` or `0.20` into an immutable, self-validating Value Object.

#### The Domain Model (`src/pricing/domain/margin.ts`)

```ts
import {Result} from '../../shared/result';
import {InvalidMarginError} from './invalid-margin-error';

export class Margin {
    private static readonly REGIONAL_RATES: Record<string, number> = {
        EU: 0.18,
        UK: 0.22,
        US: 0.15,
        APAC: 0.25,
    };
    private static readonly DEFAULT_RATE = 0.2;

    private constructor(public readonly rate: number) {
    }

    public static fromPercentage(percentage: number): Result<Margin, InvalidMarginError> {
        if (percentage < 0 || percentage > 100 || isNaN(percentage)) {
            return Result.fail(new InvalidMarginError(`Margin percentage must be between 0 and 100, received: ${percentage}`));
        }
        return Result.ok(new Margin(percentage / 100));
    }

    public static forRegion(region?: string): Margin {
        const rate = (region && Margin.REGIONAL_RATES[region]) ?? Margin.DEFAULT_RATE;
        return new Margin(rate);
    }

    public applyTo(baseAmount: number): number {
        return Math.round(baseAmount * this.rate * 100) / 100;
    }

    public equals(other: Margin): boolean {
        return this.rate === other.rate;
    }
}
```

#### Unit & Property-Based Tests (`src/pricing/domain/margin.spec.ts`)

```ts
import {describe, expect, it} from 'vitest';
import * as fc from 'fast-check';
import {Margin} from './margin';

describe('Margin Value Object', () => {
    it('creates valid margin from percentage', () => {
        const result = Margin.fromPercentage(18);
        expect(result.isOk()).toBe(true);
        expect(result.value.rate).toBe(0.18);
    });

    it('rejects invalid margin percentages', () => {
        expect(Margin.fromPercentage(-5).isFail()).toBe(true);
        expect(Margin.fromPercentage(150).isFail()).toBe(true);
    });

    // Property-Based Test (PBT)
    it('property: applyTo always produces non-negative markup for positive amounts', () => {
        fc.assert(
            fc.property(
                fc.float({min: 0, max: 1_000_000, noNaN: true}),
                fc.integer({min: 0, max: 100}),
                (amount, percentage) => {
                    const margin = Margin.fromPercentage(percentage).value;
                    const markup = margin.applyTo(amount);
                    return markup >= 0 && markup <= amount;
                },
            ),
        );
    });
});
```

---

### 2. Participant Hands-On: Complete the Pricing Kernel (25 min)

Participants implement one or more of the following Value Objects:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CHOOSE YOUR VALUE OBJECT                        │
├───────────────────────┬───────────────────────┬────────────────────────┤
│ VO 1: Money           │ VO 2: Price           │ VO 3: VatRate          │
│ amount + currency,    │ wraps Money, adds     │ per country, per goods/│
│ no business rule      │ pricing invariants    │ services category      │
│ add / multiply /      │ (e.g. strictly        │ (e.g. FR: 20% / 10% /  │
│ equals                │ positive)             │ 5.5% / 2.1%)           │
└───────────────────────┴───────────────────────┴────────────────────────┘
```

#### Exercise A1a: `Money` Value Object (`src/pricing/domain/money.ts`)

The simpler VO: amount + currency, no pricing-specific business rule.

- **Invariants**: Amount must be non-negative, currency code (ISO 3-letter, default `EUR`), rounded to 2 decimal places.
- **Operations**: `add(other: Money)`, `multiply(factor: number)`, `equals(other: Money)`.
- **Factory**: `Money.create(amount: number, currency?: string): Result<Money, InvalidMoneyError>`.

#### Exercise A1b: `Price` Value Object (`src/pricing/domain/price.ts`)

A richer VO built on top of `Money`, carrying pricing-specific invariants. Pick this one if you want to practice
modeling a domain concept that *wraps* a simpler VO rather than reinventing its arithmetic.

- **Invariants**: Wraps a `Money`; must be strictly positive (a price of zero or negative is invalid), tied to a catalog
  context.
- **Operations**: `add(other: Price)`, `multiply(factor: number)`, `equals(other: Price)`.
- **Factory**: `Price.create(amount: number, currency?: string): Result<Price, InvalidPriceError>`.

#### Exercise A2: `VatRate` Value Object (`src/pricing/domain/vat-rate.ts`)

A VAT rate is not a single flat number: it depends on the **country** and, within a country, on the **category of
goods/services**. France alone has four standard rates (20% standard, 10% and 5.5% reduced, 2.1% super-reduced), each
applying to different product categories.

- **Invariants**: Rate between `0.0` (0%) and `0.30` (30%); tied to a `country` code and a `category` (or "standard"
  default) so the same `VatRate` type can't silently mix France's 5.5% (food) with Germany's 19% (standard).
- **Operations**: `calculateTax(taxableAmount: number): number`.
- **Factory**:
  `VatRate.create(rate: number, country: CountryCode, category?: VatCategory): Result<VatRate, InvalidVatRateError>`.

#### Exercise A3 (Integration -- Optional): `ResellerPriceCalculator`

- Combine `Price`, `Margin`, and `VatRate` into a pure domain function:
  $$\text{ResellerPrice} = \text{BasePrice} + \text{MarginAmount} + \text{BaseVAT} + \text{VATOnMargin}$$

---

## 🛡️ Phase B — Rich Aggregates & Invariants (50 min)

Participants select **one track** according to their interest:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   CHOOSE YOUR BOUNDED CONTEXT TRACK                    │
├────────────────────┬────────────────────┬──────────────────────────────┤
│ Track 1: Inventory │ Track 2: Catalog   │ Track 3: Procurement         │
│ `StockItem`        │ `CatalogItem`      │ `RegionalSupplier`           │
│ Reservation logic  │ Slugs & Statuses   │ Region & Capacity policies   │
└────────────────────┴────────────────────┴──────────────────────────────┘
```

---

### Track 1: Inventory Bounded Context (`src/inventory/`)

**Legacy Problem**: `reserveStock` blindly decrements a database column with race conditions and missing invariants.

1. **Model the Aggregate** (`src/inventory/domain/stock-item.ts`):
    - Identity: `StockItemId` / `ProductId`
    - State: `availableQuantity: number`, `reservedQuantity: number`
    - Invariants: `availableQuantity >= 0`, `reservedQuantity >= 0`
    - Methods:
        - `reserve(qty: number): Result<void, InsufficientStockError>`
        - `release(qty: number): Result<void, InvalidReservationError>`
        - `restock(qty: number): Result<void, InvalidRestockError>`
2. **PBT Invariant**:
    - For any sequence of valid reservations and releases:
      $$\text{currentAvailable} + \text{currentReserved} == \text{initialStock}$$

---

### Track 2: Catalog Bounded Context (`src/catalog/`)

**Legacy Problem**: Product title and slug are coupled to database columns with unvalidated image JSON blobs.

1. **Model the Aggregate** (`src/catalog/domain/catalog-item.ts`):
    - Value Objects: `Slug`, `ProductTitle`, `ImageCollection`
    - State: `status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'`
    - Invariants:
        - Cannot publish a product without a valid slug and at least one image.
        - Slugs must be lowercase, alphanumeric with hyphens, length $\ge 3$.
    - Methods:
        - `publish(): Result<void, CatalogPublishError>`
        - `updateSlug(newSlug: Slug): Result<void, CatalogError>`
        - `archive(): Result<void, CatalogError>`

---

### Track 3: Procurement Bounded Context (`src/procurement/`)

**Legacy Problem**: Suppliers and regions stored as loose JSON dictionary (`suppliersRegions: Record<string, unknown>`).

1. **Model the Aggregate** (`src/procurement/domain/regional-supplier.ts`):
    - Value Objects: `SupplierId`, `Region`, `LeadTimeDays`
    - Invariants:
        - A supplier can only serve supported regions.
        - Minimum Order Quantity (MOQ) must be $> 0$.
    - Methods:
        - `assignRegion(region: Region, leadTime: LeadTimeDays): Result<void, ProcurementError>`
        - `deactivateRegion(region: Region): Result<void, ProcurementError>`

---

## 🔬 Phase C — The Verification Gate (15 min, optional)

Are we happy with our harness ?

What do you prefer ? PBT ? Mutation testing ? Agentic review ? Human review ?

Run the automated verification suite against your new domain models:

### 1. In-Memory Unit & PBT Suite

```bash
pnpm test
```

- All domain tests execute in **< 100ms** without spinning up Docker or hitting PostgreSQL.

### 2. Mutation Testing Gate

```bash
pnpm test:mutation
```

- Compare the new score against the legacy baseline:
    - **100% Mutation Score on domain files** (all mutant boundary shifts `<` vs `<=`, arithmetic offsets `+` vs `-`
      killed).

---

## 🏆 Phase D: Retrospective & CUPID Checklist (5 min)

Review the resulting domain code against the **CUPID properties**:

| Property                | Checkpoint                                                                    | Achieved? |
|:------------------------|:------------------------------------------------------------------------------|:---------:|
| **C — Composable**      | Pure Value Objects and Aggregates compose cleanly without DI framework glue.  |    ✅     |
| **U — Unix Philosophy** | Each Aggregate / VO does one domain job well with clear boundaries.           |    ✅     |
| **P — Predictable**     | Invariants enforced by construction; `Result` monad makes failures explicit.  |    ✅     |
| **I — Idiomatic**       | TypeScript immutable classes, factory patterns, fast in-memory Vitest runner. |    ✅     |
| **D — Domain-Based**    | Ubiquitous Language in code (`Margin`, `StockItem`, `Slug`), not table names. |    ✅     |
