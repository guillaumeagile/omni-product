# Skill: mutation-testing-verification

## Use when

- unit and property-based tests already exist
- you need evidence that tests detect subtle logic changes
- arithmetic, boundary, or conditional logic is business-critical

## Goal

Use mutation testing as a quality gate, not as a vanity metric.

## Do

- run mutation testing on stable, isolated domain code first
- inspect surviving mutants one by one
- strengthen tests with missing properties or boundary examples
- target arithmetic, comparisons, and branch behavior
- keep the feedback loop focused on business-critical paths

## Avoid

- adding shallow assertions just to kill mutants
- mutating broad infrastructure code before domain logic is solid
- treating mutation score as more important than signal quality
- hiding design problems behind excessive test setup

## Deliverables

- a mutation report with surviving mutants explained
- targeted test improvements
- clear mutation scope and gate expectations

## Done when

- important mutants are killed by behavior-focused tests
- surviving mutants are either removed by better tests or consciously justified
- the test suite proves it would catch meaningful logic drift
