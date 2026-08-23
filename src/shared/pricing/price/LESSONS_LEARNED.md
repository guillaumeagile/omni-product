# Lessons learned — src/shared/pricing/price

## TDD + mutation testing is not enough — you also need PBT

This module was built by the book: `tdd-by-the-book` (red/green/refactor, one plain-English rule at a time from
`price.spec.md`), then verified with Stryker mutation testing scoped to the file. Both passed at 100%. Both gave false
confidence.

Once property-based tests (`price.properties.spec.ts`, using `fast-check`)
were added on top, one immediately failed: `withTax` is not reliably monotonic in the tax rate near rounding boundaries
when rates are close together (e.g. `lowerRate ≈ 0.01` vs `lowerRate + 0.01`, at certain base amounts). No example-based
test had ever hit this case, and no mutant surfaced it either.

**Why each layer missed it, and what the next layer caught:**

- **TDD by the book** only writes the example cases a human thought to name in `*.spec.md`. It's excellent at pinning
  down *known* rules (positivity, the 100000 ceiling, the [0,1] tax range) but structurally cannot find a bug that
  requires an input nobody enumerated.
- **Mutation testing (100% score)** only proves the *existing* assertions aren't dead code — every mutant introduced
  into `price.ts` was caught by some example test. It says nothing about whether those assertions are actually true
  across the input domain. A property can be wrong and still kill every mutant.
- **Property-based testing** is what actually found the bug, because it searches the input space (via `fc.float`
  generators) instead of relying on examples a human picked. It found a genuine edge case in under 40 random trials.

**Takeaway for future domain types in this codebase:** treat
`tdd-by-the-book` + Stryker as necessary but not sufficient. For any type with a continuous numeric domain (money,
rates, percentages) — especially one involving rounding — write PBT properties (invariants, ordering, idempotence)
*before* declaring the type done, the same way this project already does for `Margin` (see `margin.spec.ts`). Skipping
that step is what let this bug through two rounds of by-the-book verification.

## Secondary note: a failing property can itself have a bug — investigate before loosening it

The monotonicity property (`withTax is monotonic in the rate for a fixed price`) failed for a real reason, but not the
one first assumed. The counterexample was `[amount ≈ 0.01, lowerRate ≈ 0.9900000095367432]`. Two things were wrong, and
only one was about production rounding:

1. Rounding to 2 decimals can genuinely make two close rates round in a way the raw `>=` comparison doesn't expect —
   fixed with a small `ROUNDING_TOLERANCE`, mirroring `margin.spec.ts`'s own workaround.
2. The actual trigger of *this specific* counterexample: `lowerRate + 0.01 = 1.0000000095367432`, which is **outside the
   valid `[0, 1]` tax range** the property assumed both rates would stay within. `withTax` correctly returned
   `Err` for the "higher" rate; the property's `higher.isOk() && …` then short-circuited to `false`. This wasn't a
   rounding-tolerance problem at all — the generator needed to stop below `0.99` so `lowerRate + 0.01` can never exceed
   `1.0`.

Both fixes landed in the test only; `price.ts` was untouched. The lesson: when a PBT property fails, get the actual
counterexample and compute what happens by hand before picking a fix. A rounding-tolerance patch would have looked
plausible and *seemed* to work, but the real bug (an out-of-range rate feeding a comparison that assumed both sides were
`Ok`) needed a different fix (bounding the generator), and only showed up because a fresh run with fresh random seeds
continued to intermittently fail even after adding the tolerance.

## The real finding: the plain-English spec was incomplete, not just the tests

The deeper issue PBT exposed is that `price.spec.md` never said anything about rounding-boundary behavior for
`withTax`. Rules 1-8 cover positivity, the 2-decimal/rounding rule for `Price.create`, the 100000 ceiling, and the
`[0, 1]` tax-rate range — but nothing about what guarantee (if any) holds when two *different but close* tax rates are
applied to the same price. The monotonicity property was testing a guarantee no one had actually specified in plain
English; it turned out the real guarantee is weaker ("ordered rates that differ enough to matter at the cent level
produce ordered amounts", not "any two distinct rates, however close, always order strictly"). `price.spec.md`
has been updated with a new rule 9 to state this explicitly, so the executable property and the plain-English spec now
agree.

**This is the general lesson, not a one-off:** when a property-based test fails and the fix is to loosen or narrow what
the test checks, that is a change to what the code's behavior is specified to guarantee — the same kind of change as
adding or narrowing an example-based test. It must be reflected back in `*.spec.md`, not just in the
`*.ts` test file. Fixing the test alone (as was initially done in this session, before this note) leaves the plain-
English spec silently out of sync with what's actually being verified — the exact drift `tdd-by-the-book` exists to
prevent for example-based tests, that had not been extended to property-based ones. `agents/skills/tdd-by-the-book.md`
has been updated to require this explicitly.
