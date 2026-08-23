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

## Secondary note: a green mutation score is not a green property

Once the PBT properties existed, the monotonicity property itself turned out to be too strict (it doesn't account for
rounding-boundary effects with adjacent floats — see `margin.spec.ts`'s own workaround using coarse integer steps
instead of adjacent floats for the same kind of property). That's a separate, smaller lesson: writing a PBT property is
not automatically correct just because it looks reasonable — it still needs the same rounding- aware care as the
production code it's testing. But the larger point stands:
the process gap that mattered was not having PBT in the loop at all, not the exact shape of the first property once
added.
