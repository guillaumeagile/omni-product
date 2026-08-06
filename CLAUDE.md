# Project conventions

## One object/interface/record per file

Each TypeScript `interface`, `type` alias for an object shape, or DTO/record-like
class gets its own file, named after the type (e.g. `CreateProductInput` lives in
`create-product-input.ts`). Do not bundle multiple type/interface declarations
into a service, controller, or module file — import them instead.

This does not apply to classes with behavior (services, controllers, entities
with methods) — only to plain data shapes.
