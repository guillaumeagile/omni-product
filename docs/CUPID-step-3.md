### Step 3: Functional style, pipelines and composition

Step 2 made the model immutable and gave it types that say what can happen: records, sealed results, a `switch` the
compiler checks. Step 3 is about the code that **moves data through that model**. It should read like a chain of small
steps, each one a value you can name, test and recombine, instead of a recipe of branches and accumulators.

This is the **Composable** and **Idiomatic** part of CUPID: a `Stream` or a Vavr `Option` is what Java and its usual
libraries already offer for "do this, then that", and small pure steps compose where a loop with a counter does not.

**Where you start.** The phase 3 starting code has done steps 1 and 2: the model is immutable, `Product` carries a
`ShelfLife` ADT, the sale checks `canSell` before it withdraws, the model never reads the clock. `./harness.sh 3` runs
the rules of steps 1 and 2 too, and they are green. Two rules are red, and they are the exercise.

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

#### What the harness checks

The two rules never pin a type name (except the factory names `create`, `build`, `of`, `from`, `make`), so you are free
to choose yours. A *value object* here is a `record` that does not implement `Identifiable`.

| Rule | Checks | Level |
|---|---|---|
| HA14 | A collection is transformed by a Stream pipeline (`stream().filter(...).map(...).toList()`, `Collectors.groupingBy`, `reduce`), never by a loop (`for`, `while`, `forEach`) that fills a list, a map, a builder or a counter declared outside it. | error |
| HA15 | A value object factory (static, named `create`/`build`/`of`/`from`/`make` or returning the record) validates by composing `Option`/`Validation`/`Either` steps (`filter`, `flatMap`, `map`, `combine`): no `if`, `?:`, `switch`, `throw`, `assert` or guard call in it nor in a private helper it calls; a factory whose return type can fail must hold at least one lambda or method reference. At least one such factory must exist (a generic or sealed result): without one the rule is red, like HA4.4. | error |

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

## Beyond step 3 (preview): Vavr

`Either` chains results with `map` and `flatMap`, and it is consumed with `fold`. It comes with its own rules to come
(no `get()`, no `getLeft()`). The sealed results of step 2 stay valid.
