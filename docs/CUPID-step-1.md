### Step 1: Single Responsibility → Unix Philosophy
**"One class, one reason to change"**

**Problem**: `Product` handles catalog (slug, discounts, images), pricing (`Price`), storage (stock, quantity,
warehouse), supplier (`supplier`, `suppliersRegions`) and transport (kilos, volume) — all at once. `Supplier` holds
its `Product`s and even takes services in its constructor. The two entities know the database (JPA annotations),
reference each other, and identify themselves with a `String`.

**Plan**:
- Identify every distinct concern currently living in `Product` (and `Supplier`)
- Extract each concern into its own class with a single focus
- Use the domain vocabulary to name each class (not `ProductCatalogManager` — just `Catalog`)



## **Exercise**

extract a Product smallest entity that fits the Storage BC

in the Storage sub domain, keep only properties that are useful for stocking the merchandise in a Warehouse

same for catalog, pricing, supplier notification, and transport

The code you work on lives in `src/main/java/com/omniproduct/model` (the entities) and
`src/main/java/com/omniproduct/service` (the services). The `domain/` package (carbon port) and
`infrastructure/` are not part of the exercise.

## How to work: one pull request per baby step

Each baby step goes in its own pull request, so you can see how much code each rule really costs:

- **One PR = one rule turned green.** Run `./harness.sh`, pick one `[FAIL]` line, fix only that and open a PR. If
  you are tempted to fix two rules at once, split the work into two PRs.
- **Stack the PRs.** Branch each step from the previous one (`step-1/ha9-identifiable` from `step-1/ha8-no-jpa`), and
  target the previous branch, so that each diff shows one step and nothing else.
- **Name the rule in the title**: `HA9 green: Product and Supplier implement Identifiable`.
- **Paste the harness output in the description**, before and after: the rule that turned green, and any rule
  that turned red because of it (that happens, say so rather than hiding it).
- **Read the diff before merging.** Small or large? Did it touch services too? Was the class split, or only
  renamed? Answering these questions is how you learn the refactoring, not just the green light.
- **Never touch `harness/`** in a PR.

### The game

Every pull request is refereed by a GitHub Action (`harness-game`). It runs the harness on the base branch and on
your PR, then comments your PR with a scorecard: the rules that moved, your points, your level and your badges.

- **Branch** `team-<name>/step-1/ha<N>-<what>`, for example `team-red/step-1/ha9-identifiable`. The first segment is
  your team on the leaderboard.
- **Target**: the rule in the PR title (`HA9 green: ...`), or in the branch name (`ha9`, `ha4-3`). Without a target,
  the PR is off game.
- **Accepted** when the target turns green, no other rule regresses, the project compiles and `harness/` is
  untouched. Otherwise the check fails: push a fix, the scorecard updates.

| You... | Points |
|---|---|
| turn the target green | +10 |
| resolve a warning (`[WARN]`) | +3 |
| remove violations on a rule that is still red | +1 each, up to +5 |
| turn exactly one rule green (a real baby step) | +5 |
| keep the diff small (400 changed tokens or less in `src/main/java`) | +3 |
| remove tokens from `model/` and `service/` | +1 per 100 tokens, up to +5 |
| remove cyclomatic complexity from `model/` and `service/` | +1 per 5 points, up to +5 |
| break a rule that was green | -5 each |

The code is measured in **tokens**, not lines: tokens are what an agent reads and writes, and a long line costs
more than a short one. Tokens are counted by [JTokkit](https://github.com/knuddels/jtokkit) with the `o200k_base`
encoding (the tiktoken BPE): Claude's own tokenizer is not public, but the size of a change and its trend are faithful. Cyclomatic complexity is measured by
[PMD](https://pmd.github.io/pmd/pmd_rules_java_design.html#cyclomaticcomplexity). A rise in complexity is shown
in the scorecard but never penalized: splitting a class sometimes moves a branch rather than removing it.

Badges: 🍼 Baby step, 🧼 No regression, 🤏 Small diff. After each merge, the
pinned issue **CUPID workshop leaderboard** ranks the teams. Run `./harness.sh` locally before pushing: the referee
uses the same harness.

## Assessment

run your code through our harness to verify you succeed this part:

```
./harness.sh
```

Deterministic and AST-based (see `harness/`), not AI-guided - same code in, same pass/fail out, every time. The
source is parsed with [tree-sitter](https://tree-sitter.github.io/tree-sitter) and the compiled classes are checked
with [ArchUnit](https://www.archunit.org). Rule implementations and their pinning tests live in
`harness/src/main/java/com/omniproduct/harness/rules/` and `harness/src/test/java/com/omniproduct/harness/rules/`.

The script compiles the project first: a project that does not compile never passes. Exit code `0` means every
check passes, `1` that at least one fails, `2` that the harness could not run (bad usage, nothing compiled).
`[WARN]` lines never fail the harness. `./harness.sh --test` runs the harness' own tests.

Do not relax or rewrite the harness rules to make the exercise pass - that defeats the assessment. If a rule
genuinely looks wrong, flag it.

### HA5 - no god class

no class shall have more than 4 public properties in `com.omniproduct.model`. A property is a public field, a
public `getX()`/`isX()` accessor, or a record component. Properties named `id` or ending in `Id` are ignored: they
are identifiers, not behavioural surface.

### HA6 - Single Concern

a class's public methods must belong to at most one bounded-context concern (Catalog, Pricing, Storage, Supplier,
Lifecycle, Notification, Transport - see `harness/src/main/resources/ubiquitous-language-map.json`). A method is
assigned to a concern by matching its name against that concern's vocabulary, once its leading verb (`get`, `set`,
`find`, `create`, ...) is removed: `getSlug` belongs to Catalog, `setStock` to Storage. Generic names (`getId`,
`getName`, `findById`) belong to no concern. A class whose public API spans two or more concerns is mixing
responsibilities - the exact `Product` smell this exercise targets.

The map also lists, per type name, vocabulary that must never creep back into that type: once `Slug` has been moved
out of `Product`, `Product.getSlug` fails the harness.

### HA7 - Fan-out

applies to entities - classes in `com.omniproduct.model` that implement `Identifiable` (see HA9) or declare an `id`
field. An entity may reference NO other entity type (strict cap of 0) - via fields, method or constructor
parameters, return types, or generic arguments such as `List<Product>`; entities relate by identifier (e.g.
`SupplierId`) only. Value objects (records, classes without identity), services and JDK types don't count. A class
nested in an entity counts as that entity. An entity wiring together many unrelated entities is orchestrating too
many concerns even if it stays under the HA5 property cap or doesn't trip HA6's vocabulary check.

### HA8 - No Persistence in Domain

a domain model must not know how it is stored. Any class in `com.omniproduct.model` depending on
`jakarta.persistence.*` (`@Entity`, `@Table`, `@Id`, `@Column`, `@OneToMany`, `@Embedded`, ...) or on Spring Data
fails the harness, whatever the number of other concerns it mixes in. Move the mapping into dedicated
infrastructure types (JPA entities or mapping classes living outside the model) instead.

### HA9 - Entities must implement `Identifiable`

closes a loophole HA7 would otherwise leave open. Every class declared in `com.omniproduct.model` must implement an
interface named `Identifiable`. Records are the value objects of the model: they have no identity and are exempt,
as are interfaces and enums. If a class in the model is really a value object, make it a record; if it's an entity,
implement `Identifiable`.

### HA10 - No services in entity constructors

an entity (a model class implementing `Identifiable` or declaring an `id` field, see HA9) must not take a service -
a type living under `com.omniproduct.service` or simply named `*Service` - as a constructor parameter. An entity
models data and identity; it should be constructible from plain values, value objects, or other entities. Accepting
a service inverts the dependency direction the workshop is teaching - services depend on entities, never the other
way round. Move the orchestration into the service layer instead.

### HA11 - Domain concepts are not raw primitives

a member of the model (field, parameter, record component or method return) whose name designates a concept of the
ubiquitous language (`harness/src/main/resources/ubiquitous-language-map.json`, e.g. `slug`, `price`, `quantity`,
`kilos`) must not be a raw `String`, `int`, `long`, `Double` or `BigDecimal`. Give it a domain type: a record holding
the value, e.g. `record Price(BigDecimal amount)`. Generic names (`name`, `description`) are out of scope, and
identifiers belong to HA12.

### HA12 - Identifiers are rich objects (UUID v7+ or ULID)

every `id` / `*Id` / `*Ids` field, local variable, record component, getter return type or parameter of the model
(anything outside the `service` package) must be a `UUID` or a `Ulid`, or a record wrapping exactly one of them
(`record ProductId(UUID value)`). A `String`, `int` or `long` identifier is a violation. For `*Ids` collections and
maps the element and key types are checked. "At least v7" is checked by shape: `UUID.randomUUID()` (v4) and
`new UUID(...)` are flagged where they build an identifier; a v7 passes.

The JDK has no UUID v7 generator: add a library such as
[uuid-creator](https://github.com/f4b6a3/uuid-creator) (`UuidCreator.getTimeOrderedEpoch()`) or
[ulid-creator](https://github.com/f4b6a3/ulid-creator) (`UlidCreator.getUlid()`).

**HA12.1 (warning only, `[WARN]`):** primitive obsession on identifiers, services included. Any id member whose type
is not declared in the analysed source (so a JDK or library type used raw: `String`, `int`, `UUID`, `Ulid`, `URI`,
...) gets a warning asking for a domain record, so a `ProductId` cannot be mixed up with a `SupplierId`. A `String`
id therefore both fails HA12 and warns in HA12.1.
