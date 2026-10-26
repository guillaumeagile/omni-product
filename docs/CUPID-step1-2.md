### Step 1.2: Composable, Value Objects and ADTs
**"Extend through composition, not modification"**

**Problem**: `Price` mixes its amount with the VAT entity, so the price cannot be computed as a chain of rules. Products
that behave differently (perishable, non-expiring) are modelled with flags or would be modelled with inheritance, and
the code that uses them has to ask what kind they are. Failures are thrown as exceptions instead of being returned.

**Plan**:
- Make `Price` an immutable value object, and drop the VAT entity from it
- Keep it composable: Retail Accounting computes the net price by applying the margin, then the transportation fee if
  any, then the VAT if applicable
- Model every closed set of alternatives with a `sealed interface` and records, and handle each alternative with a
  `switch`, so the compiler checks the cases
- Return expected failures as values, not exceptions

## **Exercise**

refactor `Price` to extract (and remove) the VAT entity, so that Retail Accounting is able to calculate a retail price
based on the margin.

The net price applies several rules in this order: the margin first, then the transportation fee if any, then the VAT
if applicable. An absent adjustment is simply missing from the ordered list of adjustments: no `null`, no `Optional`.

The code you work on lives in `src/main/java/com/omniproduct/model` and `src/main/java/com/omniproduct/service`, as in
step 1. Run `./harness.sh 2`: it runs the step 1 rules and the step 2 rules.

Work as in step 1: **one pull request per baby step**, each turning one HA rule green, as a **draft** PR
targeting `java-bdxio-phase2`, never merged, each branch built on the previous step (see "How to work" and "The game"
in `CUPID-step-1.md`). The diff of each PR is the lesson: you see what each rule costs.

## Harness (HA)

### HA2 - immutable data structures

Model values with `record`s. An entity stays a class (it implements `Identifiable`, HA9), and every one of its fields
is `final`. Never expose a setter. No mutable `java.util` collection (`List`, `Map`, `ArrayList`, `HashMap`...) and no
array is used as a field or in a type. Use `List.of`, `Map.of` or the immutable collections
of vavr (phase 3) instead.

### HA3 - Records are Value Objects, never entities

A record never implements `Identifiable`: identity belongs to entities (HA9), and a record's job is to be a value
object: immutable, equality by value, no identity. HA2 already forbids setters and HA5 (step 1) already caps public
properties at 4; HA3 adds that a record is not an entity, and that the `valueobject` package holds records only. A
`class` declared there is a violation.

HA3 also pins the concrete deliverable of this step: a record named `Price` must exist in a `valueobject` package.
`Price` is the running value object example: refactor it to drop the VAT entity, keep it composable and keep it an
immutable record.

### ADT (algebraic data types) - covered by HA4.3

Represent closed alternatives with a `sealed interface` that lists its alternatives in `permits`, each alternative
being a record:

```java
public sealed interface PriceAdjustment permits Margin, TransportationFee, Vat {
}
```

A `switch` over the alternatives is the exhaustive match: it has one `case` per alternative, and the compiler reports
the one you forgot when you add a new alternative. Use these ADTs for expected outcomes, such as a lookup that may
find nothing, or an operation that succeeds or fails for a named domain reason. Do not encode expected absence as
`null`, and do not throw for it. Keep unexpected failures exceptional: a violated invariant is a bug, not a business
outcome.

The harness checks this through HA4.3: `Product` must have at least one property typed by a `sealed interface` of value
objects, with no setter. Record hierarchies alone do not satisfy HA4.3.

#### Exercise 📝

Use a result type to avoid exceptions where a business outcome can fail:
- `StoredProduct.withdraw` and the lookups that currently throw an exception
- use a specific sealed result such as `sealed interface WithdrawResult permits Withdrawn, InsufficientStock`, not a
  generic `Result<T, String>` nor `Result<T, Exception>`
- the failure cases are then a closed set, and a `switch` over the result is checked by the compiler

Keep exceptions for bugs and violated invariants, not for business outcomes.

Optional, low priority: `Optional` is enough for a lookup where absence is a normal answer, but a sealed result forces
the caller to handle every case.

### HA4 - CUPID principle: Composable = extend through composition, not modification

No inheritance between classes or records, verified by static code analysis. Model alternatives with a `sealed interface`
and records rather than with a class hierarchy. Interfaces are fine, `implements` is allowed, `extends` of another class
is not.

#### Why

When two kinds of product behave differently, the reflex is `PerishableProduct extends Product` and
`NonExpiringProduct extends Product`.

That extends `Product` by inheritance: every new kind adds a subclass, and every consumer that needs to know the kind
ends up with `instanceof` checks. HA4 asks for the opposite: keep `Product` as it is and **compose** it with a value
that carries the difference.

#### Exercise 📝

Products now have a shelf life, and the two kinds differ in their dates, not in what a product is:

| Kind | Dates | Meaning |
|---|---|---|
| **Perishable** | `sellByDate`, `useByDate` | After `sellByDate` the product can no longer be sold. After `useByDate` it is unsafe to consume. Invariant: `sellByDate <= useByDate`. |
| **NonExpiring** | `bestBeforeDate` | Advisory only: a quality guideline. The product can still be sold after it, but the catalog label should say so. |

1. **Create the two value objects** as immutable records in the `valueobject` package (HA2, HA3). This is what makes
   illegal combinations unrepresentable.
2. **Give `Product` one `shelfLife` property** of type `sealed interface ShelfLife permits Perishable, NonExpiring`,
   set once at creation (no setter).
3. **Validate the invariant at the boundary.** Dates are input data, so a static factory `Perishable.create(...)`
   that returns a sealed result `PerishableCreation permits Perishable, InvalidDates` is better than a constructor
   that throws (HA4.2, HA4.4). `InvalidDates` is a record, not an exception. A Vavr `Either<InvalidDates, Perishable>`
   (or `Validation`) is accepted too.
4. **Put the behaviour behind a `switch`**, so each alternative is handled and the compiler checks it:
   `Product.canSell(today)` returns `sealed interface SaleDecision permits Sellable, PastSellByDate`. A `Perishable`
   refuses the sale after `sellByDate`; a `NonExpiring` never refuses (HA4.5).
5. **Do not store expiry in a field.** Expiry depends on today's date, so compute it when asked. A stored flag would
   go stale. The lifecycle stays about the lifecycle.
6. **Do not read the clock inside the model.** Pass `today` (or inject a `Clock`) so each date case can be tested
   without waiting. `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`, `new Date()` and
   `Clock.systemUTC()` are all refused in the model (HA4.6).
7. **Wire the services.** The sale withdraws stock only when `canSell` allows it (HA4.7); `AddProduct` receives a
   `ShelfLife`; the catalog labels (`CatalogService`) match on it to show "[BEST BEFORE PASSED]" for a `NonExpiring`
   past its date. Do not add a `default` branch to these `switch`es: without it, the compiler reports every missing
   case.

**Self-check:** adding a third kind (say `Frozen`) must only require a new record, one more type in the `permits`
clause of `ShelfLife`, and the compiler pointing at every `switch` that now needs a case. If you had to edit an
existing class hierarchy, you extended by modification instead of composition.

#### What the harness checks

HA4 is verified by seven small rules, `HA4.1` to `HA4.7`. They never pin a type name (except `Product`, `canSell`,
`withdraw`, and the factory names `create`, `build`, `of`, `from`, `make`), so you are free to choose yours. A *value
object* here is a `record` that does not implement `Identifiable`.

| Rule | Checks | Level |
|---|---|---|
| HA4.1 | No class or record extends another class (interfaces and `implements` are fine). Checked on the bytecode. | error |
| HA4.2 | No `throw`, `assert` or guard call (`requireNonNull`, `checkArgument`...) in a constructor. Return a sealed result from a factory instead. | error |
| HA4.3 | `Product` has at least one property typed by a `sealed interface` of value objects, with no setter. | error |
| HA4.4 | At least one value object has a static `create`/`build`/`of`/`from`/`make` returning a sealed result whose alternatives include the value object itself and an error record, or a Vavr ADT (`Either`, `Validation`, `Try`, `Option`) whose success type is the value object. | error |
| HA4.5 | No `instanceof` or cast on a sealed type or its alternatives; no sealed result ignored as a statement; at least one `switch` covers every alternative of an ADT whose alternatives are all records. | error |
| HA4.6 | The model never reads the clock: no `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`, `new Date()`, `Clock.systemUTC()`... Pass `today` or inject a `Clock`. | error |
| HA4.7 | A sale withdraws stock only where `canSell` allowed it: inside the branch of an `if` or `switch` on the decision, inside a `map` (or other lambda) on it, or after a guard clause that returns the refusal. Calling `canSell` and ignoring its answer does not count. | error |

An error record is a record declared in the code. A warning is printed as `[WARN]` and does not fail the harness.

## Assessment

pass the harness verification:

```
./harness.sh 2
```

It runs the step 1 rules and the step 2 rules, and exits `0` only when every error passes. `./harness.sh 2 --json report.json`
writes the same verdict as a JSON report, which the game uses (see "The game" in `CUPID-step-1.md`).

Do not relax or rewrite the harness rules to make the exercise pass - that defeats the assessment. If a rule genuinely
looks wrong, flag it.

## Phase 3 (preview): functional style

Once step 2 is green, phase 3 asks for the functional style: collections are transformed by `Stream` pipelines (HA14),
and a factory validates by composing `Option`/`Validation` steps instead of branching (HA15). Its statement is
`CUPID-step-3.md`. The sealed results of this step stay valid. `Either` chains results with `map` and `flatMap`, and
it is consumed with `fold`: a bonus for phase 3, with its own rules to come (no `get()`, no `getLeft()`).
