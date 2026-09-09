# Phase 1 — Participant Instructions: Primitive Obsession → Value Objects

> **Time budget**: ~35 min (the Part 1 / Phase A slot).
> **What you build**: one new self-validating Value Object in the pricing shared kernel, verified by unit tests,
> property-based tests, and a 100% mutation score.
> **What you do *not* touch**: `src/shared/pricing/margins/` — that is the finished worked reference. Read it, copy the
> *shape*, do not copy it into your folder.

---

## Why this exercise fits our CUPID / CUTE DDD principles

Legacy `product.service.ts` is a god service over a god table — raw numbers for price, margin, discount, tax, rules
buried in method bodies. A Value Object pulls one pricing concept into the shared kernel (`src/shared/pricing/`) where
every bounded context can use it. Each constraint below is one rule from
[`agents/rules/CUTE and CUPID Code Rules.md`](../../agents/rules/CUTE%20and%20CUPID%20Code%20Rules.md):

- **Contextual** — the VO is a genuinely shared concept; `PriceWithVat`'s `MismatchedVatRate` refuses to blur a 5.5% net
  with a 20% net.
- **Ubiquitous / Expressive** — named for the business concept, methods are domain operations (`applyTo`,
  `grossAmount`), errors name broken rules. One meaning per name.
- **Testable** — pure, in-memory, no Nest/DB/clock; the spec reads in domain language and *is* the rule documentation.
- **Composable / Unix** — one file, one job; VOs compose upward into Phase 2 aggregates without framework glue.
- **Predictable** — always-valid by construction; absence (`Option`), expected failure (`Result` + tagged error), and
  defect (throw) stay separate.
- **Idiomatic / Domain-based** — `neverthrow` `Result`, branded primitive, discriminated-union errors; data and behavior
  (rounding, range check, tax) live together, not in a service.

**Why PBT + mutation testing:** properties assert each invariant across thousands of inputs; mutation testing then
checks the tests would notice a broken rule. A stubborn survivor is usually a modeling gap — an over-wide type
conflating two domain cases — so you fix the type, not the test. See
[`side-notes/agent vs mutants.md`](side-notes/agent%20vs%20mutants.md); step 4 walks you into it.

---

## 0. Verify your environment (2 min)

Everything you need is already installed and configured. Confirm it before you start writing code.

| What                          | How to check                   | Expected                                                         |
|:------------------------------|:-------------------------------|:-----------------------------------------------------------------|
| `Option<T>` + combinators     | `cat src/shared/option.ts`     | `some`, `none`, `map`, `andThen`, `match`, `unwrapOr`, … present |
| `neverthrow` (`Result<T, E>`) | `grep neverthrow package.json` | listed under `dependencies`                                      |
| `fast-check` (PBT)            | `grep fast-check package.json` | listed under `devDependencies`                                   |
| Stryker (mutation)            | `grep stryker package.json`    | `@stryker-mutator/core` + `@stryker-mutator/vitest-runner`       |
| Mutation config               | `cat stryker.config.json`      | `mutate` covers `src/shared/pricing/**/*.ts`, `break: 100`       |
| Test scripts                  | `grep -E '"test"               | "test:mutation"' package.json`                                   | `vitest run …` and `stryker run` |

Then run the baseline once so you know the tools work:

```bash
pnpm test
pnpm test:mutation
```

`pnpm test` is green. `pnpm test:mutation` reports **100%** on the existing pricing files. Your job is to keep it at
100% after adding yours.

> **Note**: the Stryker `mutate` glob already includes any new folder under `src/shared/pricing/`. You do **not** edit
> `stryker.config.json`.

---

## 1. Study the reference: `src/shared/pricing/margins/` (5 min)

Open all three files and note the pattern — this is the exact path you will follow:

- **`margin.ts`** — the Value Object:
    - `export class Margin` with a **`private constructor`** — no `Margin` can exist anywhere without passing the
      factory.
    - a **private branded type** (`Rate = number & { readonly __brand: 'Margin.Rate' }`) — *not exported*; validity is
      enforced purely by construction.
    - a **`static fromPercentage(): Result<Margin, MarginError>`** — the fallible entry point (rejects out-of-range and
      non-finite input).
    - **total** factories that cannot fail (`forRegion(region: string)`, `default()`) — they return `Margin`, not
      `Result`, because their inputs are trusted constants.
    - instance methods only: `applyTo` (closure of operations — money in, money out), `equals` (value equality).
- **`margin-error.ts`** — a **tagged union** of the failure cases, one `kind` per case, carrying the offending value.
- **`margin.spec.ts`** — ~25 tests: domain-readable example tests grouped by method, then a `describe('… properties')`
  block of `fast-check` properties (bounds, range rejection, monotonicity, reflexivity of `equals`).

Rules that apply to everything you write (`agents/skills/tactical-ddd-always-valid.md`,
`agents/rules/typescript-style.md`, the project `CLAUDE.md`):

1. **Always-valid**: private constructor, creation only through a static factory returning `Result<T, DomainError>`.
2. **Illegal states unrepresentable — including parameters**: an optional or bare-primitive parameter often means two
   domain cases are sharing one type. Split them.
3. **Zero framework pollution**: no `@nestjs/*`, no `@prisma/client`, no HTTP.
4. **One type per file**, named after the type (`DiscountError` → `discount-error.ts`).
5. **Objects for models, functions for actions**: the VO is a class; there is no service here.

---

## 2. Pick ONE exercise

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CHOOSE YOUR VALUE OBJECT                           │
├────────────────────────────────────┬────────────────────────────────────────┤
│ Exercise 1: Discount               │ Exercise 2: PriceWithVat               │
│ folder: src/shared/pricing/        │ folder: src/shared/pricing/            │
│         discounts/                 │         price-with-vat/                │
│                                    │                                        │
│ Wraps a single primitive.          │ Composes TWO domain values             │
│ Almost 1:1 with Margin — do this   │ (a net amount + a VAT rate).           │
│ one to drill the full path.        │ Do this one for composition +          │
│                                    │ cross-value invariants practice.       │
└────────────────────────────────────┴────────────────────────────────────────┘
```

Both end at the same gate: `pnpm test` green, `pnpm test:mutation` at **100%**.

---

### Exercise 1 — `Discount`

A percentage taken *off* a base amount. Directly parallel to `Margin`; the interesting part is the factory split (see
step 4).

**Files**

- `src/shared/pricing/discounts/discount.ts`
- `src/shared/pricing/discounts/discount-error.ts`
- `src/shared/pricing/discounts/discount.spec.ts`

**`Discount` (class, private constructor)**

- Private branded type for the internal fraction (e.g. `Fraction = number & { readonly __brand: 'Discount.Fraction' }`).
  **Not exported.**
- `static fromPercentage(percentage: number): Result<Discount, DiscountError>`
    - reject non-finite input
    - valid range **`[0, 90]`** — `0` is a legitimate "no discount"; above `90` is a data-entry error
- `static forTier(tier: string): Discount` — total. Looks up a trusted table
  (`BRONZE: 0, SILVER: 5, GOLD: 10, PLATINUM: 15`). An unrecognized tier resolves to the no-discount case.
- `static none(): Discount` — the 0% discount, for when no tier applies.
- `applyTo(baseAmount: number): number` — returns the **discounted amount** (`base − base × fraction`), rounded to 2
  decimals. Money in, money out.
- `equals(other: Discount): boolean` — value equality on the fraction.

**`DiscountError` (tagged union)**

- `{ kind: 'DiscountPercentageOutOfRange'; percentage: number }`
- `{ kind: 'DiscountPercentageNotFinite'; percentage: number }`

**Properties to cover in `fast-check`** (in addition to example tests per method)

- `applyTo` never leaves `[0, baseAmount]` for a non-negative base (allow a small rounding tolerance).
- A 0% discount is the identity: `applyTo(x) === round2(x)`.
- `applyTo` is **monotonically decreasing** in the discount rate for a fixed positive base.
- `fromPercentage` rejects every value outside `[0, 90]`.
- `equals` is reflexive for any valid discount.

---

### Exercise 2 — `PriceWithVat`

A pre-tax ("net") amount that carries the VAT rate that applies to it, and can derive the tax and gross figures. The
first VO here that *composes* other values instead of wrapping one primitive.

**Files**

- `src/shared/pricing/price-with-vat/price-with-vat.ts`
- `src/shared/pricing/price-with-vat/price-with-vat-error.ts`
- `src/shared/pricing/price-with-vat/price-with-vat.spec.ts`

**`PriceWithVat` (class, private constructor)**

- Holds a `netAmount` and a VAT rate. Model the rate as a **private branded percentage type** (not exported); covering
  the EU band `[0, 27]` is a reasonable range.
- `static of(netAmount: number, vatRatePercentage: number): Result<PriceWithVat, PriceWithVatError>`
    - reject non-finite `netAmount`, non-positive `netAmount` (a price of zero or below is invalid)
    - reject `vatRatePercentage` outside `[0, 27]`
- `taxAmount(): number` — `round2(netAmount × rate)`.
- `grossAmount(): number` — `netAmount + taxAmount()`. Closure of operations: stays in money terms.
- `add(other: PriceWithVat): Result<PriceWithVat, PriceWithVatError>` — defined **only** when both carry the same VAT
  rate; otherwise a tagged `MismatchedVatRate` error. (You cannot add a 5.5% net to a 20% net and still have one
  well-defined VAT rate — this is the "two domain cases sharing one type" trap, surfacing in an operation.)
- `equals(other: PriceWithVat): boolean` — **both** the net amount **and** the rate must match.

**`PriceWithVatError` (tagged union)**

- `{ kind: 'NetAmountNotPositive'; netAmount: number }`
- `{ kind: 'NetAmountNotFinite'; netAmount: number }`
- `{ kind: 'VatRateOutOfRange'; vatRatePercentage: number }`
- `{ kind: 'MismatchedVatRate'; left: number; right: number }`

**Properties to cover in `fast-check`** (in addition to example tests per method)

- `grossAmount() >= netAmount` always; `taxAmount() >= 0` always.
- `grossAmount() === round2(netAmount)` **iff** the rate is 0.
- `add` is commutative when the rates match: `a.add(b)` and `b.add(a)` yield equal results.
- `add` fails with `MismatchedVatRate` whenever the rates differ.
- `equals` is reflexive; two `PriceWithVat` with the same net but different rates are **not** equal.
- `of` rejects every `netAmount <= 0`, every non-finite `netAmount`, and every rate outside `[0, 27]`.

---

## 3. The path (follow it in order)

1. **Model the type** — write the class and the private branded type. Constructor private. No logic in the constructor
   beyond storing the already-valid value.
2. **Write the error union** — one file, one `kind` per failure case, each carrying the offending value.
3. **Fallible factory** — `fromPercentage` / `of`. Guard clauses return `err({ kind: … })`; the happy path returns
   `ok(new …(…))`.
4. **Total factories** — `forTier` / `none` (Exercise 1). Trusted constants only, so they return the bare type.
5. **Instance methods** — `applyTo` / `taxAmount` + `grossAmount` + `add`, and `equals`. Rounding helper: `round2`.
6. **Example tests** — one `describe` per method, domain-readable names ("rejects a percentage above 90", not
   "returns err"). Use a small `ok…()` helper in the spec to unwrap a known-valid instance, like `okMargin` in
   `margin.spec.ts`.
7. **Property tests** — a `describe('… properties')` block, the properties listed for your exercise.
8. **Run `pnpm test`** — green.
9. **Run `pnpm test:mutation`** — drive it to **100%, zero survivors**.

---

## 4. If a mutant survives

That is the point of the exercise. Before you rewrite a test, ask whether the surviving mutant is pointing at the
*type*, not the test suite.

The reference `Margin` had exactly one stubborn survivor: a `region === undefined` guard on a `forRegion(region?:
string)` signature. Four test/code rewrites failed to kill it. The real defect was the **optional parameter** — it
conflated "resolve a named tier's rate" with "give me the default", two domain cases in one signature. Splitting them
into `forRegion(region: string)` + a separate `default()` made the branch structurally impossible and the mutant had
nothing left to survive as.

- **Exercise 1**: your `forTier` unknown-tier handling is the analogous spot. Keep `forTier(tier: string)` and
  `none()` as two distinct factories — do not fold them into one optional-parameter function.
- **Exercise 2**: watch the `add` rate-comparison branch and the `grossAmount` rate-zero path.

Full account: [`side-notes/agent vs mutants.md`](side-notes/agent%20vs%20mutants.md).

---

## Definition of done

- [ ] New folder under `src/shared/pricing/` with three files: the VO, its error union, its spec.
- [ ] Private constructor; the branded internal type is not exported.
- [ ] `pnpm test` green; ~20–25 tests including a property block.
- [ ] `pnpm test:mutation` at **100.00%**, zero survivors.
- [ ] No `@nestjs/*`, `@prisma/client`, or HTTP imports in the VO or its error file.
- [ ] You did not modify `src/shared/pricing/margins/` or `stryker.config.json`.
