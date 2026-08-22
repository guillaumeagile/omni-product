# Skill: tdd-by-the-book

## Use when

- the user asks to implement something "using TDD"
- writing a new domain type, method, or behavior test-first
- any task explicitly framed as red/green/refactor

## Goal

Follow the TDD loop exactly, one behavior at a time, so every line of production
code exists because a failing test demanded it — never ahead of it.

## The loop

1. **Red** — write **one** small test for a behavior that does not exist yet.
   Run it. Confirm it fails, and confirm *why*: a missing module/class/method,
   not a typo or a broken test harness.
2. **Green** — write the **minimum** production code to make that one test pass.
   Do not implement invariants, branches, or parameters no test has asked for
   yet, even if the spec/doc mentions them. "Fake it" (e.g. return a constant,
   skip validation) is an acceptable and often correct green step.
3. **Refactor** — with the test green, clean up duplication or naming in either
   the test or the production code. Re-run to confirm still green. No behavior
   change.
4. **Verify** — run mutation testing (`pnpm test:mutation`) scoped to the file(s)
   just touched. A surviving mutant means the code contains a branch, condition,
   or literal no test pins down. Report the surviving mutant(s) and **stop** —
   do not add a test to kill it without discussing the fix first (it may call
   for a test, or for removing untested code instead). Do not proceed to the
   next test while mutants survive.
5. Repeat: pick the next smallest untested behavior and go back to step 1.

## Do

- write only one failing test before writing any implementation
- run the test and observe the actual failure before writing a line of production code
- treat "it compiles/passes on the first try" as a signal something is wrong —
  either the test isn't asserting the right thing, or code was written ahead of it
- let each subsequent test drive one new invariant, branch, or edge case
  (e.g. reject negative amount, reject bad currency, rounding) — not all at once
- keep steps small enough that the diff for "green" is obvious and minimal
- narrate the cycle explicitly: which step (red/green/refactor/verify) is happening now
- run mutation testing after each green step to prove no untested code was written
- when a mutant survives, report it and propose a fix, then wait for confirmation
  before writing the killing test or removing the untested code

## Avoid

- writing a full implementation (all invariants from a spec/doc) after a single test
- writing the test and the implementation in the same turn without running red first
- adding validation, error cases, or methods that no test currently requires
- treating a design document's list of invariants as a checklist to implement
  upfront instead of as a backlog of future failing tests
- skipping the "confirm it fails" step
- adding a test to kill a surviving mutant without first discussing it with the user

## Deliverables

- one new failing test, run and confirmed red, before any implementation
- minimal implementation making only that test pass, run and confirmed green
- a mutation testing run on the touched file(s) with no surviving mutants
- an explicit next-smallest-test proposed for the following cycle

## Done when

- every production line traces back to a test that failed before it existed
- no untested invariant or branch exists in the code
- mutation testing confirms zero surviving mutants on the code just written
- the test suite reads as an incremental log of the behaviors built
