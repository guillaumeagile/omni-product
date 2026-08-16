# Skill: nest-cupid-architecture

## Use when

- organizing modules, layers, and dependencies
- separating domain, application, and infrastructure concerns
- moving a NestJS codebase away from god services and god DTOs

## Goal

Apply CUPID and ports-and-adapters structure without losing NestJS ergonomics.

## Do

- organize code by bounded context and business capability
- keep controllers thin and focused on HTTP concerns
- map DTOs to commands and domain outputs to response shapes
- define repository ports in the inner layers and adapters in infrastructure
- isolate Prisma and NestJS from the domain model
- follow `CLAUDE.md`: one object/interface/record-like type per file
- keep dependency wiring in Nest modules, not in domain code

## Avoid

- domain services that import framework or ORM types
- controllers that contain business rules
- monolithic schemas mirroring database tables
- direct coupling from domain code to Prisma persistence details
- folder structures driven only by technical layers when business boundaries matter more

## Deliverables

- clear module and folder boundaries
- explicit ports, adapters, and mappers
- thin delivery layer and framework-free domain code
- files that each carry one clear responsibility

## Done when

- domain logic is portable and testable without Nest bootstrapping
- replacing persistence or transport changes outer layers more than inner layers
- the code structure reflects business language and boundaries
