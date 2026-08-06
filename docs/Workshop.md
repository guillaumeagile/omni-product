# CUTE DDD + CUPID + Model Exploration Whirlpool Workshop

> **Unified Workshop Framework** combining CUTE DDD principles, CUPID properties, and Eric Evans' Model Exploration Whirlpool for joyful, domain-driven software development.

---

## 🎯 Workshop Objectives

This workshop provides a **holistic approach** to domain-driven design by integrating three complementary frameworks:

| Framework | Focus | Origin |
|-----------|-------|--------|
| **CUTE DDD** | Strategic & tactical DDD principles | Community-driven |
| **CUPID** | Code properties for joyful development | Dan North |
| **Model Exploration Whirlpool** | Collaborative model discovery process | Eric Evans / Kenny Baas-Schwegler |

**Goal**: Transform a legacy Spring Boot + JPA codebase into a well-structured, domain-aligned, testable, and maintainable system through collaborative modelling and incremental refactoring.

---

## 📐 Part 1: Foundational Principles

### CUTE DDD — Strategic Design Principles

| Letter | Principle | Description |
|--------|-----------|-------------|
| **C** | **Contextual** | Design decisions based on specific domain context; align code structure with bounded contexts |
| **U** | **Ubiquitous** | Shared language understood by all stakeholders; use domain terms in code |
| **T** | **Testable** | Easy to test at all levels (unit, integration, acceptance); design for testability from the start |
| **E** | **Expressive** | Clearly communicates business intent; intent-revealing names, self-documenting structure |

### CUPID — Tactical Code Properties (Joyful Code)

| Letter | Property | Description | DDD Connection |
|--------|----------|-------------|----------------|
| **C** | **Composable** | Plays well with others; small, focused components that combine easily | Aggregates, Domain Events, Value Objects compose naturally |
| **U** | **Unix Philosophy** | Does one thing well; single-purpose modules with clear boundaries | Bounded Contexts, Single Responsibility per aggregate |
| **P** | **Predictable** | Does what you expect; no surprising side effects | Invariants, Always-Valid Entities, Domain Events |
| **I** | **Idiomatic** | Feels natural in the language/framework; leverages platform strengths | Spring Boot idioms, Java records for VOs, Pattern Matching |
| **D** | **Domain-Based** | Code models the problem domain in language and structure | Ubiquitous Language, Tactical DDD patterns |

> **Key Insight**: CUPID properties are *centred sets* (direction of travel) not *bounded sets* (pass/fail). CUTE principles guide *what* to build; CUPID properties guide *how* to build it.

---

## 🌀 Part 2: The Model Exploration Whirlpool (Process)

> **Eric Evans' Model Exploration Whirlpool** — A collaborative, iterative process for discovering and refining domain models. Not a development process, but a *modelling process* that fits within any development process.

### Phase Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    MODEL EXPLORATION WHIRLPOOL                   │
├──────────────────┬──────────────────┬───────────────────────────┤
│  HARVEST &       │  MODEL, SLICE,   │  CONTINUOUS               │
│  DOCUMENT        │  FORMALISE &     │  REFINEMENT               │
│  (As-Is → To-Be) │  CODE PROBE      │  (Whirlpool cycles)       │
└──────────────────┴──────────────────┴───────────────────────────┘
```

### Phase 1: Harvest & Document — Make the Implicit Explicit

#### 1.1 Process EventStorming (As-Is)
- **Participants**: Domain experts, developers, UX, product
- **Output**: Timeline of Domain Events (orange stickies)
- **Techniques**: 
  - Tell the story — enforce timeline, remove duplicates
  - Mark **Hotspots** (pink) for disagreements/unknowns
  - Make implicit explicit with EventStorming colours:
    - 🔵 **Blue**: Commands/Actions
    - 🟣 **Long Pink**: External Systems
    - 🟣 **Long Lilac**: Policies/Eventual Business Constraints
    - 🟢 **Green**: Information/Read Models

#### 1.2 Example Mapping (As-Is) — Battle Cognitive Bias
- **Why**: Counter confirmation bias, attentional bias, bias blind spot
- **Technique**: Write "Friends episodes" — "The one where..."
- **Output**: Concrete examples revealing gaps in As-Is understanding
- **Integration**: Cross-reference with EventStorm; mark new hotspots

#### 1.3 Process EventStorming (To-Be)
- **New modelling space** — fresh paper roll
- **Storm Domain Events** for desired future state
- **Walkthrough & Refocus**: Enforce timeline, find the **hard part** (complexity)
- **Introduce Business Rules** (Long Yellow stickies):
  - Consistent business rules (always before domain event)
  - Eventual consistent business rules
  - One rule per sticky for later refactoring
- **Add Actors** — make responsibilities visible

#### 1.4 Example Mapping (To-Be) — Structure by Business Rules
- **Vertical rows** per business rule (blue sticky header)
- **One business rule per row** — examples may repeat across rules
- **Discover new rules** → feed back into EventStorm
- **Formalise examples** as acceptance criteria (Gherkin-style)

> **Duration**: 2–3 hours for As-Is; 2–3 hours for To-Be. Sleep on it between phases.

### Phase 2: Model, Slice, Formalise & Code Probe

#### 2.1 Explore Multiple Models
- **Generate at least 3 candidate models** quickly
- **Test each model** against:
  - EventStorm timeline (does it explain all events?)
  - Example Map (does it satisfy all business rules?)
- **Select workable model** — not perfect, but testable

#### 2.2 Slice & Formalise
- **Prioritise business rules** — which are most critical?
- **Formalise examples** as executable specifications
- **Bounded Context Canvas** extended with BDD scenarios

#### 2.3 Code Probe (Outside-In TDD)
- **Start with formalised examples** as coarse-grained tests
- **Implement domain model** using TDD
- **Continuously refine** ubiquitous language & challenge model
- **Fast feedback** — inspect & adapt design

### Phase 3: Continuous Refinement (The Whirlpool)
- **Repeat cycles** as new insights emerge
- **Each cycle**: Harvest → Model → Code → Reflect
- **Bounded contexts evolve** — split/merge as understanding deepens
- **Living documentation**: EventStorms, Example Maps, Bounded Context Canvases

---

## 🏗️ Part 3: Strategic Changes — Applying CUTE DDD to Omni-Product

### Business Context (From Legacy Analysis)

Legacy Spring Boot + JPA codebase with mixed concerns:
- **Product** (transport, stock, catalog frontend, suppliers all embedded)
- **Suppliers Management**
- **Stock Management**
- **Catalog Frontend**
- **Transport Management**
- Inter-dependent models with DB relationships

### New Strategic Requirements

| Feature | Domain Implication |
|---------|-------------------|
| Multiple suppliers per region | Supplier → Region aggregate; Regional pricing policy |
| Reseller price = supplier price + margin | Pricing domain service; Margin per region |
| VAT only on margin | Tax calculation as domain service/value object |
| Slug depends on frontend | Slug generation decoupled (IoC); Frontend-specific |
| Frontend catalog modifications (images) | Catalog BC separate from Inventory/Procurement |
| Frontend field filtering | API projection / Read Model per consumer |

### Strategic Application of CUTE DDD

#### 1. Contextual Design → Bounded Context Discovery
```
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Procurement    │  │   Inventory      │  │    Catalog       │
│  (Suppliers)    │  │   (Stock)        │  │   (Frontend)     │
└────────┬────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
                    ┌─────────────────────┐
                    │   Pricing & Tax     │
                    │   (Shared Kernel)   │
                    └─────────────────────┘
```

- **Procurement BC**: Supplier onboarding, regional supplier management, purchase orders
- **Inventory BC**: Stock levels, reservations, deprecation, stock movements
- **Catalog BC**: Product presentation, images, slugs, frontend projections
- **Pricing & Tax**: Shared kernel — margin policies, VAT calculations, price composition

#### 2. Ubiquitous Language → Domain Vocabulary
| Technical Term | Domain Term |
|----------------|-------------|
| `Product` | `CatalogItem` / `StockItem` / `ProcurementItem` (per BC) |
| `Supplier` | `Vendor` / `RegionalSupplier` |
| `margin` | `RegionalMarginPolicy` |
| `slug` | `FrontendIdentifier` |
| `VAT` | `MarginTaxCalculation` |

#### 3. Testable Models → Test Strategy per BC
- **Unit Tests**: Aggregates, Value Objects, Domain Services (fast, isolated)
- **Integration Tests**: Repository adapters, Saga/Process Managers
- **Acceptance Tests**: Example Map scenarios → Gherkin → Cucumber/SpecFlow
- **Contract Tests**: Between BCs (Consumer-Driven Contracts)

#### 4. Expressive Code → Intent-Revealing Structure
```
// Before (Anemic)
Product.setPrice(supplierPrice + margin);

// After (Expressive, Domain-Based)
ResellerPrice.calculateFor(regionalSupplier, regionalMarginPolicy, vatRate);
// or
regionalSupplier.quoteResellerPrice(regionalMarginPolicy, vatRate);
```

---

## ⚙️ Part 4: Tactical Changes — Applying CUPID + DDD Patterns

### Code Quality Lens: CUPID Properties as Refactoring Guide

| CUPID Property | Legacy Smell | Target Pattern | Example |
|----------------|--------------|----------------|---------|
| **Composable** | God `Product` entity | Small Aggregates + Domain Events | `StockItem`, `CatalogItem`, `ProcurementItem` |
| **Unix Philosophy** | Services doing everything | Single-purpose Domain Services | `PricingService`, `TaxCalculation`, `SlugGenerator` |
| **Predictable** | Nulls, exceptions, invalid states | Always-Valid Entities, Result/Either | `Result<Price>`, `Option<StockLevel>` |
| **Idiomatic** | Manual DTO mapping, heavy ORM | Java Records, Pattern Matching, Spring Data projections | `record Price(...)`, `interface StockRepository` |
| **Domain-Based** | Technical names (`ProductDTO`) | Domain names (`ResellerQuote`, `StockReservation`) | Ubiquitous language in code |

### Tactical Refactoring Roadmap

#### Phase A: Eliminate Primitive Obsession (Domain-Based + Predictable)
- [ ] `Price` → Value Object with currency, amount, precision
- [ ] `Margin` → Value Object with percentage, region
- [ ] `VatRate` → Value Object with country, rate
- [ ] `Slug` → Value Object with frontend context
- [ ] `Region` → Value Object (not String)
- [ ] `SupplierId`, `ProductId`, `StockId` → Typed IDs

#### Phase B: Rich Domain Model (Composable + Domain-Based)
- [ ] **Aggregates**: `RegionalSupplier`, `StockItem`, `CatalogItem`
- [ ] **Entities**: `Supplier`, `Product` (per BC)
- [ ] **Value Objects**: `Price`, `Margin`, `VatRate`, `Slug`, `Region`
- [ ] **Domain Events**: `SupplierOnboarded`, `StockReceived`, `ProductDeprecated`, `PriceChanged`
- [ ] **Invariants**: Enforced in constructors/factory methods

#### Phase C: Decouple Infrastructure (Unix Philosophy + Composable)
- [ ] **Ports & Adapters**: Repository interfaces in domain, implementations in infrastructure
- [ ] **DTO Mapping**: Explicit mappers per use case (no auto-mapping)
- [ ] **Slug Generation**: Strategy pattern (IoC) — `SlugGenerator` interface
- [ ] **ORM → Projections**: Spring Data JPA projections for read models

#### Phase D: Validation vs Invariants (Predictable + Domain-Based)
- [ ] **Input Validation**: At application boundary (request DTOs)
- [ ] **Business Invariants**: Inside aggregates (always valid)
- [ ] **Result/Either**: For operations that can fail (no exceptions for control flow)

#### Phase E: Event-Driven Integration (Composable + Unix Philosophy)
- [ ] **Domain Events** published from aggregates
- [ ] **Event Handlers** for cross-BC communication
- [ ] **Saga/Process Manager** for multi-BC workflows (e.g., deprecation flow)
- [ ] **Outbox Pattern** for reliable event publishing

---

## 🗺️ Part 5: Workshop Execution Plan

### Session 1: Harvest & Document (4 hours)
| Time | Activity | Output |
|------|----------|--------|
| 0:00–0:15 | Intro, domain context, EventStorming basics | Shared understanding |
| 0:15–1:15 | Process EventStorming (As-Is) | Domain Event timeline |
| 1:15–1:30 | Tell story, mark hotspots | Refined timeline |
| 1:30–2:00 | Make implicit explicit (coloured stickies) | Rich EventStorm |
| 2:00–2:15 | Break |
| 2:15–3:00 | Example Mapping (As-Is) — battle bias | Concrete examples, gaps |
| 3:00–3:30 | Process EventStorming (To-Be) | Future Domain Events |
| 3:30–4:00 | Walkthrough, business rules, actors | To-Be EventStorm |

### Session 2: Model & Formalise (3 hours)
| Time | Activity | Output |
|------|----------|--------|
| 0:00–0:30 | Example Mapping (To-Be) — structure by rules | Business rules + examples |
| 0:30–1:30 | Explore 3+ candidate models | Model sketches |
| 1:30–2:00 | Test models against EventStorm & Examples | Selected workable model |
| 2:00–2:30 | Slice Example Map, formalise as Gherkin | Acceptance criteria |
| 2:30–3:00 | Extend Bounded Context Canvas with scenarios | Living documentation |

### Session 3: Code Probe & Refactor (Ongoing — 2-week sprints)
| Sprint | Focus | CUPID Target |
|--------|-------|--------------|
| 1 | Primitive Obsession → Value Objects | Domain-Based, Predictable |
| 2 | Aggregate design, invariants | Composable, Domain-Based |
| 3 | Ports & Adapters, Repository pattern | Unix Philosophy, Composable |
| 4 | Domain Events, cross-BC integration | Composable, Predictable |
| 5 | Slug strategy, read models, projections | Idiomatic, Unix Philosophy |
| 6+ | Continuous refinement (Whirlpool cycles) | All properties |

---

## 📋 Part 6: Legacy Analysis — Current Codebase Mapping

### Controllers (Mixed Concerns)
| Controller | Current Responsibilities | Target BC |
|------------|-------------------------|-----------|
| `ProductController` | Transport, stock, catalog, suppliers | Split across BCs |
| `SupplierController` | Supplier CRUD | Procurement |
| `StockController` | Stock levels, movements | Inventory |
| `CatalogController` | Frontend product data | Catalog |
| `TransportController` | Shipping, logistics | Procurement/Inventory |

### Services (Anemic, Inter-dependent)
| Service | Issues | Refactoring Target |
|---------|--------|-------------------|
| `ProductService` | God service, all concerns | Decompose per BC |
| `SupplierService` | Mixed persistence/logic | Procurement domain service |
| `StockService` | Direct DB access | Inventory aggregate + repository |
| `CatalogService` | Frontend coupling | Catalog read model projector |

### Domain Model (JPA Entities)
| Entity | Problems | Target |
|--------|----------|--------|
| `Product` | Anemic, all concerns, DB-coupled | Split: `StockItem`, `CatalogItem`, `ProcurementItem` |
| `Supplier` | Mixed regions, no invariants | `RegionalSupplier` aggregate |
| `Stock` | No encapsulation | `StockItem` with reservations |
| `Transport` | Embedded in Product | Separate BC or value object |

---

## ✅ Part 7: Definition of Done — Workshop Success Criteria

### Strategic (CUTE DDD)
- [ ] **Bounded Contexts** identified, documented, and agreed with domain experts
- [ ] **Ubiquitous Language** glossary created and used in code
- [ ] **Context Maps** drawn (Partnership, Shared Kernel, Customer-Supplier, etc.)
- [ ] **Test Strategy** defined per BC (pyramid respected)

### Tactical (CUPID + DDD Patterns)
- [ ] **Primitive Obsession** eliminated — domain primitives as Value Objects
- [ ] **Aggregates** enforce invariants — always valid, no setters
- [ ] **Domain Events** published for significant state changes
- [ ] **Ports & Adapters** — domain independent of infrastructure
- [ ] **No Anemic Models** — behavior co-located with data
- [ ] **Result/Either** used for fallible operations — no control-flow exceptions

### Process (Model Exploration Whirlpool)
- [ ] **EventStorm** (As-Is & To-Be) documented and photographed
- [ ] **Example Maps** formalised as executable specifications
- [ ] **Bounded Context Canvases** extended with BDD scenarios
- [ ] **Code Probes** implemented for core domain — TDD from examples
- [ ] **Whirlpool Cadence** established — monthly model refinement sessions

---

## 📚 References & Further Reading

- **Model Exploration Whirlpool**: [Kenny Baas-Schwegler — WeaveIT](https://weave-it.org/2019/02/04/model-exploration-whirlpool-domain-driven-design-the-first-15-years/)
- **CUPID Properties**: [Dan North — cupid.dev](https://cupid.dev/)
- **EventStorming**: [Alberto Brandolini — Introducing EventStorming](https://leanpub.com/introducing_eventstorming)
- **Example Mapping**: [Cucumber.io — Example Mapping](https://cucumber.io/docs/bdd/example-mapping/)
- **Bounded Context Canvas**: [Nick Tune — Bounded Context Canvas](https://www.nicktune.co/bounded-context-canvas/)
- **DDD Reference**: [Eric Evans — Domain-Driven Design](https://domainlanguage.com/ddd/)
- **CUTE DDD**: Community principles for Contextual, Ubiquitous, Testable, Expressive DDD

---

> **Workshop Mantra**: *"Harvest the implicit, make it explicit. Model multiple ways, probe with code. Keep whirling toward deeper insight."}]</parameter=explanation=
