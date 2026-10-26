---
name: harness-rule-maker
description: Create, change or review a rule of the deterministic workshop harness (harness/ module, rules HAx, tree-sitter AST + ArchUnit). Use when asked to add a rule, port a rule from the C# Cupid.Harness, make a rule less easy to evade, or wire a rule into a workshop step.
---

# Harness rule maker

The harness (`harness/`, run with `./harness.sh [step]`) grades the refactoring workshop on the legacy code in
`src/main/java/com/omniproduct/model` and `service`. It is **deterministic** (same code in, same verdict out, no AI).
Rules are in `harness/src/main/java/com/omniproduct/harness/rules/`, steps in `Steps.java`, the inventory in
`docs/harness-rules.md`, the plan in `docs/PLAN-harness.md`, the student-facing text in `docs/CUPID-step-N.md`.

## Non-negotiables

1. **A rule is a heuristic, not a string match.** Never rely on one exact name, type or package. A rename, a
   different casing or separator, a plural, another verb or suffix must not be enough to evade it. Reason on **words**
   (`Words.of`, `Words.isIdentifier`), combine several independent signals (name, package, suffix, annotation, types
   in generics, bytecode dependencies) and prefer "anything that is not X" over a blacklist of forbidden names.
   Combining signals must lower **both** error kinds: harder to evade (fewer false negatives) and fairer (a single
   weak signal never condemns, so fewer false positives). A trivial false negative or a false positive is a defect of
   the rule. Document the remaining limits in `docs/harness-rules.md`.
2. **Never fix the model to turn the harness green.** `model/` and `service/` are *deliberately poorly designed*. A
   harness that finds problems there proves it works; a harness that is green on the legacy is suspicious. The only
   allowed edit of legacy code is an explicit exercise change requested by the user (e.g. `Supplier` taking services
   in its constructor so HA10 fails).
3. **Functional style everywhere** in harness code: streams, `Optional`, immutable records, small pure methods. No
   index loops, no mutable accumulators, no `null` returns.
4. **One rule at a time**, each proven on the real classes before the next one.
5. **Never present an unrun test as passed.** Local build/test is allowed here; `claude --cloud` verification can only
   be launched by the user in an interactive terminal, so report it as not run.

## Choose the engine

| Need | Engine |
|---|---|
| Source shape, names, modifiers, expressions, exact line numbers | AST: tree-sitter via `SourceFile`/`Node` (`HarnessRule.check(SourceSet)`) |
| Semantics: inheritance, dependencies, resolved types, annotations | ArchUnit bytecode through `ArchFindings.of(sources, archRule)` and `ArchConditions.eachViolation` |
| Both | Run both and concatenate the `Violation`s (see `NoPersistenceInDomainRule`) |

ArchUnit reports line 0 for classes/fields/annotations; `ArchFindings` recovers the line from the AST. Tree-sitter
offsets are UTF-8 bytes (text is read from the byte array, already handled by `SourceFile`).

## Workflow for a new rule

1. **Read the source of truth**: the C# rule in `/Users/guillaume/SOURCES/for SAM 2025/Cute-KanDDDinsky-2026-CUPID-phase1/sources`
   (`Cupid.Harness`) and its `CUPID-step-N.md`. Port the *executable* behaviour, not only the doc (the doc said 6
   public properties for HA5, the code used 4: we kept the code).
2. **Write the contract first** as a failing test: a `good` fixture and a `bad` fixture in
   `harness/src/test/java/com/omniproduct/harness/fixtures/<rule>/{good,bad}/model/` (compiled, because bytecode rules
   need classes; use `Fixtures.load("ha10.bad")`) or inline sources with `SourceSet.ofSources(Map, null)` for pure AST rules.
3. **Implement the rule** as a `final class XxxRule implements HarnessRule` (`id()`, `name()`, `severity()` if
   it is a `WARNING`, `check(SourceSet)` returning `List<Violation(filePath, line, message)>`). The header comment
   states the rule, its engine and its heuristics (comments in French, matching the existing files).
   Messages say what is wrong and how to fix it, with the real `File.java:line`.
4. **Write the evasion tests** (`HeuristicEvasionTest`): for every signal, the most obvious way around it (renamed
   verb, casing, plural, suffix, generic wrapper, visibility, static import, fully-qualified name) and assert the rule
   still catches it. Add the matching good case to prove there is no obvious false positive.
5. **Prove it on the real code**, not only on fixtures:
   - run `./harness.sh` and read the findings of *your* rule on `model/` and `service/`: right class, right line,
     plausible count (cross-check with an independent `grep` when it is a count);
   - if the legacy has no violation (rule that only an exercise change can trip), do a **mutation**: copy the file to
     the scratchpad, introduce the violation, run `./harness.sh`, confirm detection, restore the file, check
     `git status` shows no change to `model/`/`service/`.
   A rule green on the legacy without a mutation proof is not finished. Make the proof **replayable**: put the mutant
   under `harness/mutations/<name>/src/...` (same path as in the repo, it must compile) with an `expected.txt` of
   `<rule> <file>:<line>` lines. `harness/mutate.sh` applies it, runs the harness, checks that each expected rule
   reports that exact line (scoped to the rule's own block), then restores `src/`; the `le-maitre-des-jeux` workflow
   runs it on every animateur PR.
6. **Wire it** into the right step in `Steps.java` (step 1 = HA5 to HA12.1; HA1, HA2 and HA4 are outside step 1). Perimeters:
   `com.omniproduct.model` and `com.omniproduct.service`, not `domain/` nor `infrastructure/`.
7. **Update the documents**: the table and the heuristics section of `docs/harness-rules.md`, the student text
   `docs/CUPID-step-N.md` (describe only what the harness really does), the plan if the scope moved.
8. **Run everything**: `gradle :harness:test --offline`, then `./harness.sh` (exit 0 pass, 1 failure, 2 unusable or
   nothing compiled; never a false PASS when classes are missing). `./harness.sh --test` runs the harness' own tests.

## Reviewing an existing rule for evasions

Ask, for each comparison in the rule: "what is the cheapest rename that makes this pass while keeping the smell?"
Typical holes: exact id names (`productID`, `product_id`, `identifier`), a closed list of bad types, only `getX`
accessors, only `public` members, `Service` suffix only, a single package, direct (non-generic) types only, data flow
not followed, fully-qualified names, static imports. Fix by words and signals, then add the evasion test.

## Git hygiene

- Stay on the branch the user chose (`java-bdxio`); no PR unless asked; push to origin.
- Commit with **explicit paths** (`git commit -- harness docs/harness-rules.md`), never `git add docs` or `-A`: the
  working tree may hold the user's own staged changes (e.g. the `docs/ADR-001-testing-strategy.md` deletion) and IDE
  noise (`.idea/gradle.xml`).
- Use `gradle` (system, JDK 21), not `./gradlew`: `gradle-wrapper.jar` is not versioned.
