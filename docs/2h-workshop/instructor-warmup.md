# Instructor Warmup: One Event, Two Bounded Contexts

> **Purpose**: before running the Phase 2 lab (`phase2-cross-bc-events.md`), walk the exact path the participants
> will walk — design an event, code the producer in one BC and the consumer in another, wire them through the
> existing `src/shared/events` bus, and pass the mutation gate. The goal isn't the code; it's discovering **where
> you hesitate**, because every hesitation of yours is a place participants will stall three times longer.

**The one discipline for this warmup**: keep both aggregates deliberately skeletal. The exercise is the *seam*, not the
aggregates — if you catch yourself adding `release()` or `ImageCollection`, you've drifted into Phase B.

House rules apply throughout: `neverthrow` `Result`, one type per file, zero `@nestjs/*` imports under `domain/`, and
the Boundary Rule (a consumer imports the producer's **event type only**).

---

## Step 0 — Design on paper first (no IDE)

Write the event as a sticky before any code: **name, payload, owner**. Force yourselves through the same debrief you'll
run in Part 2 of the lab:

- Past tense? Domain language? (`StockDepleted`, not `StockUpdateNotification`.)
- Thin? `productId` + `occurredAt`. Argue about `remainingQuantity` for thirty seconds, then drop it.
- Decide the event-name string convention now: `inventory.stock-depleted` — context-prefixed, kebab-case.
- Name the edge on a context map, out loud: Inventory is **upstream**, Catalog **downstream**; the boundary pattern is
  **Published Language** (the event) delivered through an **Open-host Service** (the bus), read by Catalog behind an
  **Anticorruption Layer** (the policy handler translating "stock" language into "availability" language). If you can't
  say that sentence smoothly, that's the rehearsal gap — see the Context Mapping appendix in
  `phase2-cross-bc-events.md` for all nine patterns and where each already lives in this codebase.

This step is the actual dress rehearsal for the lab's contract-design segment.

---

## Step 1 — Producer side, test-first

Files:

- `src/inventory/domain/events/stock-depleted.ts` — implements `DomainEvent`.
- `src/inventory/domain/stock-item.ts` — minimal: `productId`, `availableQuantity`, one method
  `reserve(qty): Result<void, InsufficientStockError>`, a private `events: DomainEvent[]`, and
  `pullDomainEvents()` draining via `splice(0)`.

Write these tests **before** the aggregate — they're the interesting ones:

1. Reserving the last unit records **exactly one** `StockDepleted`, with the right `productId`.
2. Reserving with stock remaining records none.
3. `pullDomainEvents()` **drains** — a second call returns `[]`. (This kills the `slice`-vs-`splice` mutant class:
   a copy that doesn't clear would re-publish the same fact on the next pull.)
4. A failed reservation records nothing.

---

## Step 2 — Consumer side, test-first

Files:

- `src/catalog/domain/catalog-item.ts` — skeletal: `productId`, `status`, `markUnavailable()`. Nothing else.
- `src/catalog/domain/catalog-item-repository.ts` — interface: `byProductId(id): Option<CatalogItem>` (use the existing
  `Option` from `src/shared/option.ts`), `save(item)`.
- `src/catalog/application/when-stock-depleted.ts` — the policy: load, translate, `markUnavailable()`, save.

**This last file contains your only cross-BC import** — the event type. Check it explicitly: if it imports
`StockItem`, stop and fix before moving on.

Tests:

1. A published item becomes unavailable when the policy handles `StockDepleted`.
2. An unknown `productId` is a silent no-op — not every stocked product is catalogued.

---

## Step 3 — Wire the seam through the real bus

One end-to-end test, no mocks:

1. In-memory repository holding a published `CatalogItem`.
2. `bus.subscribe('inventory.stock-depleted', (e) => policy.handle(e))`.
3. `stockItem.reserve(lastUnit)` → `bus.publish(stockItem.pullDomainEvents())`.
4. Assert the catalog item changed.

Then the **failure-isolation rehearsal**: add a throwing subscriber *before* the real one and assert the catalog still
updated. That is the lab's Part 5 demo, practiced end to end.

---

## Step 4 — The gate

1. Add `src/inventory/**` and `src/catalog/**` to `stryker.config.json`'s `mutate` array (same include +
   `!**/*.spec.ts` exclude pattern as the existing `events` entries).
2. `pnpm test`, then `pnpm test:mutation`.

Expect at least one survivor on the first pass — likely the `=== 0` boundary in `reserve`, or the no-op path in the
handler. **Don't be annoyed; write down which mutant survived and why.** That survivor is teaching material:
it's exactly the moment you want to reproduce live with participants. (Precedent: the bus itself had one — the
`?? []` fallback in `dispatch` — until a test asserted on the *absence* of phantom handler calls.)

---

## Instructor debrief — the actual point of the warmup

Note where *you* hesitated:

- The event name or its payload?
- What the handler does when the item isn't found?
- Whether the aggregate or the application service publishes?
- Where the subscription gets wired?

Each hesitation marks a spot where your Mode B live-demo narration needs to be sharpest. Bring the list to the prep
session and rehearse those transitions specifically.
