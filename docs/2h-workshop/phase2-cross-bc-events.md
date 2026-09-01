# Phase 2 Lab (50 min): Cross-BC Communication — The Event Is the Contract

> **Premise**: Two Bounded Contexts are (almost) complete from the Phase B tracks — **Inventory** (`StockItem`) and
> **Catalog** (`CatalogItem`) — each internally CUPID, always-valid, framework-free. They are autonomous islands.
> **Goal**: Make them collaborate *without re-coupling them*. The lesson: **the event is the contract, not the model.**

---

## ⏱️ Schedule (50 Minutes)

```
00:00 - 00:08 (08 min) | Part 1: The Policy Sticky — "How would you wire this?"
00:08 - 00:15 (07 min) | Part 2: Design the Contract on Paper
00:15 - 00:18 (03 min) | Part 3: The Bus Reveal — a CUPID "I" Tension (discussion)
00:18 - 00:40 (22 min) | Part 4: Code Probe (two run modes, see below)
00:40 - 00:48 (08 min) | Part 5: Verification Gate + Failure-Isolation Demo
00:48 - 00:50 (02 min) | Part 6: CUPID Retro + Stretch Pointer
```

---

## 🧭 Foundational Rules (carried over from the 2h lab)

1. **Always-Valid Models via Result** — no exceptions for expected business failures.
2. **Zero Framework Pollution in Domain** — `src/<context>/domain/` imports nothing from `@nestjs/*` or
   `@prisma/client`.
3. **One Type Per File** (see `../../CLAUDE.md`).
4. **New for this lab — the Boundary Rule**: a Bounded Context may import another BC's **event types only**. Never its
   aggregates, services, or repositories. Direction matters: the *consumer* imports the *producer's*
   published events; the producer knows nothing about who listens.

---

## 🎯 The Three "Aha" Moments (instructor engineering notes)

The session is built to trigger three realisations, in order:

1. **The tempting wrong way.** When stock hits zero, the catalog must react. The reflex is
   `catalogService.markUnavailable(...)` called from inside `InventoryService` — a direct import that re-fuses the two
   BCs the group just spent 50 minutes separating. Let them feel that pull before offering the event.
2. **Thin vs fat events.** First drafts of `StockDepleted` will carry the product title, images, maybe the price. The
   argument "what does the consumer *actually need*, and who owns this schema?" **is** the published-language lesson.
   Target: past-tense name, IDs + quantities, producer owns it.
3. **Translation at the boundary.** The catalog handler receives `StockDepleted` (inventory language) and calls
   `catalogItem.markUnavailable()` (catalog language). Different words on purpose — that is an Anti-Corruption Layer in
   miniature. The consumer never touches the producer's aggregate.

---

## 🗣️ Part 1: The Policy Sticky (8 min)

### The wider landscape, first

Before narrowing to today's exercise, put the whole picture on the wall for a minute. This isn't a two-BC system — it's
(at least) four, and Inventory↔Catalog is only the pair we have time to *code* today:

```
┌───────────────────┐                  ┌──────────────┐                  ┌──────────────┐
│    Procurement     │  StockReceived   │   Inventory   │  StockDepleted   │   Catalog    │
│ RegionalSupplier   │ ───────────────▶ │   StockItem   │ ───────────────▶ │ CatalogItem  │
└───────────────────┘                  └───────┬───────┘                  └───────┬──────┘
                                                │                                  │
                                                │ StockReserved                    │ ItemPublished
                                                ▼                                  ▼
                                  ┌───────────────────────────────────────────────────────┐
                                  │                    Sales / eCommerce                    │
                                  │            Order, Cart — reacts to both sides           │
                                  └─────────────────────────────────────────────────────────┘
```

(event names shown here are all 🟠 orange-sticky domain events — the colour convention from Part 2 of the long-format
workshop guide)

- **Procurement** raises `StockReceived` (a purchase order lands) — Inventory reacts by restocking.
- **Inventory** raises `StockDepleted` — Catalog reacts by hiding the item (today's exercise).
- **Inventory** also raises `StockReserved` — Sales/eCommerce reacts by confirming an order line can ship.
- **Catalog** raises `ItemPublished` / `ItemArchived` — Sales/eCommerce reacts by showing/hiding the item in the
  storefront, and (stretch, Part 6) Inventory reacts to `ItemArchived` by releasing reservations.

None of these BCs calls another's service directly. Each only ever *states a fact* about itself and *reacts* to facts
from others — the same mechanism, four times over. **Today we code one edge of this graph (Inventory → Catalog) so the
pattern is fully in your hands; the other edges are the same recipe, applied again.**
If your own domain has a fifth or sixth BC, the question is never "does this scale?" — it's "what's the next fact, and
who reacts to it?"

### The pair we're coding: Inventory → Catalog

Put a single EventStorming fragment on the wall — one lilac **policy** sticky between the two BCs:

```
┌──────────────┐   🟠 StockDepleted    🟣 "Whenever stock is depleted,   ┌──────────────┐
│  Inventory   │ ────────────────────▶    hide the item from the      ──▶│   Catalog    │
│  StockItem   │                          catalog"                       │ CatalogItem  │
└──────────────┘                                                         └──────────────┘
```

Ask the room: **"How would you wire this?"** and collect answers on the board without judging:

| Proposal                                       | What it really is                                                       |
|------------------------------------------------|-------------------------------------------------------------------------|
| Update both tables in one transaction          | Shared database — the God-model, reborn                                 |
| `InventoryService` calls `CatalogService`      | Direct coupling — one deploy unit, one failure unit, cyclic-import risk |
| Inventory calls Catalog's HTTP API             | Temporal coupling — catalog down ⇒ reservation fails                    |
| Inventory publishes a **fact**; Catalog reacts | Decoupled: producer doesn't know, doesn't wait, doesn't care            |

Close with: *"Inventory's job ends when it states the fact. What happens next is someone else's policy."*

---

## 📝 Part 2: Design the Contract on Paper (7 min)

Small groups draft the `StockDepleted` event on a sticky: **name + payload fields**. Instructor plays fat-event devil's
advocate ("surely the catalog needs the title to hide it?").

Debrief targets:

- **Past tense, domain language** — `StockDepleted`, not `StockUpdateNotification`.
- **Thin payload** — `productId`, `occurredAt`. (`remainingQuantity` is debatable — by definition it's 0; keeping it is
  a nice 30-second argument.)
- **Producer owns the schema.** The event lives in `src/inventory/domain/events/stock-depleted.ts`. It is Inventory's
  *published language*; changing it is a breaking change to unknown consumers.

---

## ⚖️ Part 3: The Bus Reveal — a CUPID "I" Tension (3 min discussion)

We use a **hand-rolled ~30-line in-memory bus** (pre-built in `src/shared/events/`). Before showing it, open the tension
explicitly:

> **NestJS ships `@nestjs/event-emitter` (`EventEmitter2`) with `@OnEvent` decorators. That is the *Idiomatic*
> choice — CUPID's "I". So why are we hand-rolling one?**

Let participants argue both sides for two minutes, then land it:

|                           | Hand-rolled bus                             | `EventEmitter2` + decorators          |
|---------------------------|---------------------------------------------|---------------------------------------|
| CUPID "I" (Idiomatic)     | ✗ not what a NestJS dev expects            | ✓ platform-native                    |
| Visibility for learning   | ✓ every line on one screen, zero magic     | ✗ magic strings, decorator metadata  |
| Domain purity             | ✓ no framework import anywhere near domain | ~ handlers become framework artifacts |
| Sync, deterministic tests | ✓ trivially                                | ~ needs module setup                  |

**The point**: CUPID properties are *centred sets* — they can pull against each other, and choosing is the job. Today,
*Composable/Predictable* and pedagogy outrank *Idiomatic*. In your production NestJS codebase, the idiomatic emitter (or
a real broker + outbox) likely wins. Neither answer is "correct"; the trade-off is.

---

## 💻 Part 4: Code Probe (22 min)

### Two run modes

| Mode                                                  | Who writes what                                                                                                               | When to use                                            |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| **A — Full track**                                    | Participants write the event type, raise it in `StockItem`, write the catalog policy + tests                                  | Prep/rehearsal, strong groups, agent-assisted sessions |
| **B — Handlers only** (default for the live workshop) | Instructor live-demos the event + raising it (≈7 min); participants write **only** the consuming policy + its tests (≈15 min) | Real 50-min session — protects the clock               |

Everything below exists on the starting branch **except** the pieces the active mode assigns to participants.

### Pre-built scaffolding (`src/shared/events/`)

```ts
// src/shared/events/domain-event.ts
export interface DomainEvent {
    readonly name: string;
    readonly occurredAt: Date;
}
```

```ts
// src/shared/events/in-memory-event-bus.ts
import {DomainEvent} from './domain-event';

export type EventHandler<E extends DomainEvent> = (event: E) => void;

export class InMemoryEventBus {
    private readonly handlers = new Map<string, EventHandler<DomainEvent>[]>();

    subscribe<E extends DomainEvent>(eventName: string, handler: EventHandler<E>): void {
        const existing = this.handlers.get(eventName) ?? [];
        this.handlers.set(eventName, [...existing, handler as EventHandler<DomainEvent>]);
    }

    publish(events: DomainEvent[]): void {
        for (const event of events) {
            for (const handler of this.handlers.get(event.name) ?? []) {
                try {
                    handler(event);
                } catch {
                    // A consumer's failure is not the producer's problem.
                    // Production answer: dead-letter + retry via an outbox — see Part 5.
                }
            }
        }
    }
}
```

### Step 1 — The event (producer's published language)

```ts
// src/inventory/domain/events/stock-depleted.ts
import {DomainEvent} from '../../../shared/events/domain-event';

export class StockDepleted implements DomainEvent {
    public readonly name = 'inventory.stock-depleted';
    public readonly occurredAt = new Date();

    constructor(public readonly productId: string) {
    }
}
```

### Step 2 — The aggregate records, the application dispatches

`StockItem` gets a recording mechanism — the aggregate **states facts**, it never touches a bus (Rule 2: zero
framework/infrastructure in domain):

```ts
// inside src/inventory/domain/stock-item.ts
private readonly
events: DomainEvent[] = [];

reserve(qty
:
number
):
Result < void, InsufficientStockError > {
    // ...existing invariant checks and state change...
    if(this.availableQuantity === 0
)
{
    this.events.push(new StockDepleted(this.productId));
}
return Result.ok(undefined);
}

pullDomainEvents()
:
DomainEvent[]
{
    return this.events.splice(0);
}
```

The application service closes the loop after persisting:

```ts
// src/inventory/application/reserve-stock.ts (already wired on the branch)
const reservation = stockItem.reserve(qty);
if (reservation.isOk()) {
    await this.stockItems.save(stockItem);
    this.eventBus.publish(stockItem.pullDomainEvents());
}
```

### Step 3 — The consuming policy (participants' core work in both modes)

```ts
// src/catalog/application/when-stock-depleted.ts
import {StockDepleted} from '../../inventory/domain/events/stock-depleted'; // the ONLY allowed cross-BC import

export class WhenStockDepleted {
    constructor(private readonly catalogItems: CatalogItemRepository) {
    }

    handle(event: StockDepleted): void {
        const item = this.catalogItems.byProductId(event.productId);
        if (item.isNone()) return; // not every stocked product is catalogued — that's fine
        item.value.markUnavailable();   // catalog speaks "availability", not "stock" — translation, not forwarding
        this.catalogItems.save(item.value);
    }
}
```

(`CatalogItem.markUnavailable()` is pre-built on the branch in mode B; in mode A, participants add it.)

### Step 4 — Tests participants must produce

1. **Producer boundary test** (mode A): reserving the *last* unit raises exactly one `StockDepleted`; reserving with
   stock remaining raises none. (Kills the `=== 0` vs `<= 0` vs `< 0` mutants.)
2. **Policy test** (both modes): given a `PUBLISHED` `CatalogItem` and a `StockDepleted` event, the handler makes it
   unavailable — pure in-memory, no NestJS module, no DB.
3. **PBT (stretch)**: for any sequence of valid `reserve`/`release`/`restock` operations,
   `StockDepleted` is raised **exactly once per crossing** of the zero boundary — not on every call at zero.

---

## 🔬 Part 5: Verification Gate + Failure-Isolation Demo (8 min)

```bash
pnpm test           # all domain + policy tests, in-memory, < 100ms
pnpm test:mutation  # optional: 100% score on the new event/policy files
```

**The demo that sells the architecture** — subscribe a deliberately broken second handler:

```ts
bus.subscribe('inventory.stock-depleted', () => {
    throw new Error('analytics service is down');
});
```

Reserve the last unit. **The reservation still succeeds, the catalog handler still ran.** One BC's failure is not
another BC's failure — that is what "decoupled" *means*, mechanically.

Then one honest slide, discussion only (do **not** code it):

> In-process + try/catch means a crashed process loses events, and a swallowed error is silent. The production
> answer is the **Outbox Pattern**: events saved in the same transaction as the aggregate, relayed to a broker,
> retried, dead-lettered. Same contract, same handlers — only the transport hardens. Eventual consistency is the
> price of autonomy, and the business usually already works that way.

---

## 🏆 Part 6: CUPID Retro + Stretch (2 min)

| Property                | Checkpoint                                                                                                 |
|:------------------------|:-----------------------------------------------------------------------------------------------------------|
| **C — Composable**      | BCs now compose *through events*, not through imports. Adding a third listener costs the producer nothing. |
| **U — Unix Philosophy** | Inventory manages stock. Catalog manages presentation. The policy is the pipe between them.                |
| **P — Predictable**     | An event is an immutable past fact. No hidden side effects — reactions are explicit, named policies.       |
| **I — Idiomatic**       | Deliberately traded away today (Part 3) — and we can say *why*.                                            |
| **D — Domain-Based**    | `StockDepleted`, `markUnavailable` — the lilac sticky became a line of code you can read aloud.            |

### 🚀 Stretch goal (documented, for fast groups)

Reverse flow: `CatalogItem.archive()` raises `ItemArchived` → Inventory policy releases outstanding reservations and
stops accepting new ones.

Retro question that comes with it: *"Inventory → Catalog and Catalog → Inventory… did we just build a cycle?"*
**No** — two independent one-way policies reacting to facts, no shared transaction, no call stack spanning both BCs. A
cycle of *dependencies* is deadly; a conversation of *events* is just how businesses run.

---

## ✅ Instructor Prep Checklist (before the session)

- [ ] Starting branch has both aggregates green: `StockItem` (reserve/release/restock) and `CatalogItem`
  (publish/archive + `markUnavailable` for mode B)
- [ ] `src/shared/events/` bus + `DomainEvent` in place, with tests
- [ ] `reserve-stock` application service wired to persist-then-publish
- [ ] Mode decided (A: full track / B: handlers only) and the corresponding pieces removed from the branch
- [ ] Policy sticky fragment printed / drawn for Part 1
- [ ] Broken-handler snippet ready to paste for the Part 5 demo

---

## 📎 Appendix: Advanced Use Case — Who Calculates the Displayed Price? (Procurement → Catalog)

> **Scenario**: a new supplier is declared. It operates in a **different country with different tax rules**. The
> catalog must display a full price, tax details included. **Who is responsible for calculating it?**

Use this as a discussion appendix (or a follow-up session seed). It's a trap question, and that's the point: it *sounds*
like it has a one-BC answer. It doesn't — asked as "who calculates?", every single-BC answer is wrong in an instructive
way.

### The three wrong one-BC answers

| Tempting answer                                                       | Why it's wrong                                                                                                                                                                                                                                               |
|-----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Procurement calculates it** and puts the display price in its event | Procurement would need to know margin policies and storefront tax presentation — a responsibility leak. This is the **fat event smell at BC scale**: the producer doing the consumer's thinking. Worse: every pricing-rule change now redeploys Procurement. |
| **Catalog calculates it** from scratch                                | Catalog is a *presentation* context. If VAT regimes and margin math live in Catalog's domain, Sales/eCommerce will need the same math for the cart — copy-paste divergence guaranteed.                                                                       |
| **Catalog asks Procurement** for the price at render time             | Synchronous cross-BC query — temporal coupling, exactly the wiring Part 1 rejected. Procurement down ⇒ storefront down.                                                                                                                                      |

### The decomposition: three responsibilities, three places

The question dissolves once you split "calculate the full price" into **cost facts**, **calculation rules**, and
**display decision**:

```
┌────────────────────┐                                  ┌─────────────────────────┐
│    Procurement      │  SupplierDeclared                │        Catalog          │
│                     │  SupplierPriceChanged            │                         │
│  owns: cost price,  │ ───────────────────────────────▶ │  policy handler reacts, │
│  supplier country,  │        (facts only —             │  stores a DisplayPrice  │
│  purchase terms     │     no display price!)           │  read model (breakdown) │
└────────────────────┘                                  └───────────┬────────────┘
                                                                     │ calls (pure, in-process)
                                                                     ▼
                                                        ┌─────────────────────────┐
                                                        │  Pricing Shared Kernel   │
                                                        │  Price, Margin,          │
                                                        │  TaxPolicy.forCountry()  │
                                                        │  owns: HOW to compute    │
                                                        └─────────────────────────┘
```

1. **Procurement states cost facts.** `SupplierPriceChanged { productId, supplierId, costPrice, currency,
   supplierCountry }`. Past tense, thin, no display price. The supplier's country is a *fact about the supplier* — it
   belongs in the event. What that country *implies* for tax does not.
2. **The Pricing shared kernel owns the math.** This codebase already encodes the domain rule *reseller price = supplier
   cost + margin, VAT on the margin only* (`Price`, `Margin`, `PriceWithTax`). A supplier in a new country doesn't
   change *who* calculates — it changes **which rule the kernel selects**: VAT-on-margin regime vs. VAT-on-full-price
   vs. reverse charge for cross-border acquisition. That's a strategy keyed by country
   (`TaxPolicy.forCountry(supplierCountry, storefrontCountry)`), pure and property-testable like everything else in the
   kernel — note it needs **both** countries, which is exactly why neither BC alone could own it.
3. **Catalog decides when and for whom.** Its policy handler reacts to Procurement's fact, invokes the kernel, and
   stores a denormalized `DisplayPrice` read model carrying the full breakdown (base, margin, tax detail, applied
   regime). Presentation context owns presentation — including "we show tax details", which is a storefront choice, not
   a procurement or tax-law concern.

**One-line answer for the room**: *Procurement states what it cost. Pricing knows how to compute. Catalog decides what
to show. The calculation is **triggered in** Catalog's application layer but **owned by** the kernel.*

### The subtle domain insight worth surfacing

Displayed tax depends on the **buyer's context** (storefront country, customer location) at least as much as on the
supplier's. "New supplier country" changes the *cost side inputs* (import duties, acquisition VAT treatment); the
*sales-side* tax is a different axis entirely. Participants who notice that the tax rule is a function of **two**
countries have found the real reason this can't live in either BC — hand them a sticker.

### Where do the tax rates live?

Follow-up question with a trap of its own. The key distinction: **rules are behavior, rates are reference data.**

- The **rule** — "VAT on the margin only", "reverse charge cross-border" — is domain logic: it changes rarely and
  changes the *shape* of the calculation. It lives in the kernel as code (`TaxPolicy`, a *pure* domain service:
  regime selection given the two countries, rates passed in as values).
- The **rate** — 20% FR, 21% NL — is data with **temporal validity**: it changes by legislation on a date, and
  re-quoting an old order must reproduce the rate in force *at the time of sale*. No code change should ship because
  Spain adjusts its VAT.

So rates sit behind a **port + adapter**, same recipe as the bus:

```ts
// kernel (src/shared/pricing/tax/) — owns the shape of the question, not the data
interface TaxRateProvider {
    rateFor(country: Country, on: Date): Result<VatRate, UnknownTaxJurisdictionError>;
}
```

| Piece                    | Where                   | Workshop version                                                                                   | Production version                                                                       |
|--------------------------|-------------------------|----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `TaxRateProvider` (port) | kernel                  | same                                                                                               | same — that's the point                                                                  |
| Adapter                  | infrastructure          | `InMemoryTaxRateProvider`, hardcoded table (what `Margin` does today, but behind the interface)    | DB reference table (`country, rate, valid_from`) or external tax API; caching lives here |
| Resolution               | **application** service | Catalog's policy handler resolves the rate, passes the `VatRate` *value* into the pure kernel math | same                                                                                     |

**The anti-pattern to name out loud**: a domain-level "TaxService" that *fetches*. The moment a domain service does I/O,
the kernel stops being pure and property-testable. The application service is the bridge — exactly the role it already
plays with the `EventBus` and the repository. One policy handler, two ports in, pure math in the middle, repository out:
participants have seen this shape once already; here it is again.

**Staleness corollary**: `SupplierPriceChanged` arrives at time T; the `DisplayPrice` read model is read at T+n. If a
rate changes in between, the stored price is stale — so a rate change must itself trigger recalculation. That's the
argument for the rate table having a real owner able to raise its own fact (`TaxRateChanged`) rather than being a config
file nobody watches. (And once something *owns* rates and *publishes* facts about them, you're most of the way to the
Pricing BC of the evolution path below.)

### Evolution path (discussion only)

- **Today's size**: shared kernel as a library + Catalog-side policy. Right-sized for this system; the kernel is
  versioned with both consumers, which is the shared-kernel trade-off (tight coupling, accepted knowingly).
- **When pricing grows** (promotions, per-customer quotes, currency conversion, price history): promote Pricing to a
  full BC that subscribes to `SupplierPriceChanged` and publishes its own fact — `ResellerPriceQuoted` — which Catalog
  and Sales both consume. Same recipe as today's exercise, applied one more time: the graph grows by an edge, no
  existing BC changes its contract.
- **Deliberately out of scope**: currency conversion (a new supplier country likely means a new currency — that's a
  whole session), and per-customer tax display (B2B net vs. B2C gross).
