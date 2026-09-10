# Project conventions

## Cohesion over strict one-per-file

Each distinct domain type — `interface`, `type` alias for an object shape, or DTO/record-like class — gets its own file,
named after the type (e.g.
`CreateProductInput` lives in `create-product-input.ts`). Do not bundle unrelated type/interface declarations into a
service, controller, or module file — import them instead.

Exception: a small closed algebraic module (e.g. `Option<T>`, `Result<T, E>`
and their combinators) stays in one file. The type and its operators form one vocabulary meant to be read and imported
as a unit — splitting them fragments something that only makes sense together.

This does not apply to classes with behavior (services, controllers, entities
with methods) — only to plain data shapes.

## Tooling: ts-morph

Use `ts-morph` only for mechanical, repo-wide code transformations that ESLint autofix and editor rename can't do:
moving declarations between files while rewriting imports, changing a function's signature at every call site,
introducing a value object across many usages, or finding unreferenced exports. For anything smaller, edit by hand and
lean on `tsc` + tests. It is not a project dependency — add it under `devDependencies` and put scripts in
`scripts/codemods/` if a case actually calls for it.
