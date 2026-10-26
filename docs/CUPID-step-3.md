### Step 3: Functional style, pipelines and composition

Step 2 made the model immutable and gave it types that say what can happen: records, sealed results, a `switch` the
compiler checks. Step 3 is about the code that **moves data through that model**. It should read like a chain of small
steps, each one a value you can name, test and recombine, instead of a recipe of branches and accumulators.

This is the **Composable** and **Idiomatic** part of CUPID: a `Stream` or a Vavr `Option` is what Java and its usual
libraries already offer for "do this, then that", and small pure steps compose where a loop with a counter does not.

**Where you start.** The phase 3 starting code has done steps 1 and 2: the model is immutable, `Product` carries a
`ShelfLife` ADT, the sale checks `canSell` before it withdraws, the model never reads the clock. `./harness.sh 3` runs
the rules of steps 1 and 2 too, and they are green. Two rules are red, and they are the exercise. A warning,
`HA16`, points at the guard of `sellOne`. A third rule,
`HA21`, is green: it guards the way you use Vavr (see below) and only turns red if you open a Vavr container.

The rules of this step are numbered from `HA14`: the number 13 is skipped on purpose.

## **Exercise**

### HA14 - Collections are transformed by pipelines

`CatalogService.catalogLabels` builds the catalog labels in a loop that fills a list declared outside it. A `Stream`
says what you want instead of how to fill it:

```java
products.stream()
        .filter(...)
        .map(product -> switch (product.getShelfLife()) { ... })
        .toList();
```

Each step is a small pure function you can name and test. The behaviour is pinned by `CatalogServiceTest`: it describes
what the catalog shows, not how the code is written, so you can refactor with it as a safety net. Only the few small
factories at the bottom of that test know how to build a product.

### HA15 - A factory validates by composition

A value object is created through a static factory (HA4.4), and the factory has to say whether the input is valid. A
`if` that cuts the flow answers that question by branching:

```java
if (sellBy == null || useBy == null || useBy.isBefore(sellBy)) {
    return Option.none();
}
return Option.of(new Perishable(sellBy, useBy));
```

Compose instead: each condition is a step, the result is the chain.

```java
return Option.of(sellBy)
        .flatMap(s -> Option.of(useBy).filter(u -> !u.isBefore(s)).map(u -> new Perishable(s, u)));
```

Or, to keep the reason of the failure, `Validation.combine(...).ap(Perishable::new)`. No `if`, `?:`, `switch`, `throw`
or `requireNonNull` in the factory, nor in a private helper it calls.

**Self-check:** adding a new condition to a factory (say, `useBy` at most a year after `sellBy`) must only add one
`filter` step, not change the shape of the code.

### HA16 - A domain decision is composed, not tested (warning)

`ProductService.sellOne` asks the product whether it can be sold, then cuts the flow with a guard clause:

```java
if (!product.canSellOn(today)) {
    return Option.none();
}
return Option.of(withdrawStock(stock));
```

The decision is a value: make it a step of the chain instead of a branch, with `Option.when(decision, () -> ...)`, or
`filter`/`map`/`fold` on the result.

```java
return Option.when(product.canSellOn(today), () -> withdrawStock(stock));
```

This rule is a **warning**: it shows as `[WARN]` and never fails the harness. It is the one step-3 rule that is not
about a loop or a factory, so read it, then decide.

### HA21 - A Vavr container is consumed by `fold`, never opened

`Either`, `Option`, `Try` and `Validation` say "this may have failed" in the type. Opening them throws the information
away: `get()`, `getLeft()` and `getOrElseThrow()` throw when you guessed the wrong side, and `isLeft()`, `isRight()`,
`isEmpty()` only replay the `if` that the container exists to avoid.

```java
// refused: the caller must remember to test first
if (result.isLeft()) { return report(result.getLeft()); }
return use(result.get());

// accepted: both cases are handled, the compiler checks that none is forgotten
return result.fold(this::report, this::use);
```

`map`, `flatMap`, `filter`, `peek` and `getOrElse(value)` are fine. This rule is green when you start: it is a guard rail
for the code you write to fix HA15, not an exercise.

#### What the harness checks

The rules never pin a type name (except the factory names `create`, `build`, `of`, `from`, `make`), so you are free
to choose yours. A *value object* here is a `record` that does not implement `Identifiable`.

| Rule | Checks | Level |
|---|---|---|
| HA14 | A collection is transformed by a Stream pipeline (`stream().filter(...).map(...).toList()`, `Collectors.groupingBy`, `reduce`), never by a loop (`for`, `while`, `forEach`) that fills a list, a map, a builder or a counter declared outside it. | error |
| HA15 | A value object factory (static, named `create`/`build`/`of`/`from`/`make` or returning the record) validates by composing `Option`/`Validation`/`Either` steps (`filter`, `flatMap`, `map`, `combine`): no `if`, `?:`, `switch`, `throw`, `assert` or guard call in it nor in a private helper it calls; a factory whose return type can fail must hold at least one lambda or method reference. At least one such factory must exist (a generic or sealed result): without one the rule is red, like HA4.4. | error |
| HA16 | In `service`, no `if` whose condition carries a domain decision (a name with the word `can`, or `sellable`, `allowed`, `permitted`, `eligible`, `decide`, `decision`, directly or through a local variable) and whose branch ends in `return` or `throw`, and no such decision in a returned `?:`. Compose it with `Option.when`, `filter`, `map` or `fold`. | warning |
| HA21 | No call, and no method reference, that opens a Vavr container of `io.vavr.control` (`Either`, `Option`, `Try`, `Validation`) in `model/` and `service/`: `get()`, `getLeft()`, `getError()`, `getCause()`, anything with `throw` in its name (`getOrElseThrow`), and the tests `isLeft()`, `isRight()`, `isEmpty()`, `isDefined()`, `isSuccess()`, `isFailure()`, `isValid()`, `isInvalid()`. The check is on the compiled code and reads the method name in words, so a static import or a variable typed by a supertype does not hide the call. | error |

## Assessment

Pass the harness verification:

```
./harness.sh 3
```

It runs the rules of steps 1 and 2 and the rules of step 3, and exits `0` only when every error passes.
`./harness.sh 3 --json report.json` writes the same verdict as a JSON report, which the game uses (see "The game" in
`CUPID-step-1.md`).

Do not relax or rewrite the harness rules to make the exercise pass - that defeats the assessment. If a rule genuinely
looks wrong, flag it.

## Score in this phase

The size of the code changes from one phase to the next, so the token thresholds of the scorecard do not stay the same.
In phase 3 the starting code is small (about 1 700 tokens in `model/` and `service/`), and a baby step changes a few
hundred tokens at most:

| Bonus | Phase 3 |
|---|---|
| Small diff (changed tokens in `src/main/java`) | 200 or less: +3 |
| Tokens removed from `model/` and `service/` | +1 per 50 tokens, up to +5 |

The other points (turning a rule green, baby step, complexity, PMD) are the ones of `CUPID-step-1.md`.
