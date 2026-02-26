# the Cute DDD workshop

## Objectives 


### 🎯 CUTE DDD Principles

#### **C** - **Contextual**
Design decisions based on specific domain context


#### **U** - **Ubiquitous**
Shared language understood by all stakeholders


#### **T** - **Testable**
Easy to test at all levels (unit, integration, acceptance)


#### **E** - **Expressive**
Clearly communicates business intent

## CUTE DDD : Stragegic changes

We want to add new features
  - multiple suppliers per region
  - reseller price is based on  supplier price + margin
    - margin is per region
    - VAT must be added only on the margin
  - slug depends on frontend implementation
  - frontend team request catalog specific modifications, images for example 
  - front end must not display all product fields


### Tackle complexity : Event Storming -> Find Bounded Contexts



## Analyze legacy OO code (Spring Boot + JPA)

- several controlers and models, all mixed up
    - product (all concerns embedded: transport, stock, catalog front end, suppliers)
    - suppliers management
    - stock management
    - catalog front end
    - transport management
- several services as well
- and all models with inter-depedencies (relationships in DB)

## 🖋️ Domain StoryTelling + Context Mapping 

write stories about Products, Suppliers and Stocks

examples:

    - supplier add product to catalog, then ship later into the stock
    - product is in stock, then sold: the supplier must be advised, the stock must be updated
    - a product is in stock, but deprecated: the supplier must be advised, the stock must be updated, the customer must be advised (product label must change)

goal: split domain in bounded contexts , let events emerge 

## 🚀 Applying Stragegic changes

1. **Contextual Design**
    - Align code structure with domain boundaries
    - Make decisions based on business needs

2. **Ubiquitous Language**
    - Use domain terms in code
    - Bridge technical and business understanding

3. **Testable Models**
    - Design for testability from the start
    - Clear separation of concerns in tests
    - respect test pyramid : create unit tests

4. **Expressive Code**
    - Intent-revealing names
    - Self-documenting through structure


## CUTE DDD : tactical changes 

###  technical concerns / code easier to maintain

- Nulls and exceptions
- Mapping problem: entities exposed vs dedicated dto
- Anemic Domain Model
- too much logic in constructor (extract validation logic)
- add Slug logic ( decoupled from Slug library , use Inversion of Control)
- ORM is heavy, use DTOs
- Port and adapters for persistence 

### so many changes ! so little time !
- Primitive Obsession
- Aggregates, Entities, Value Objects
- Events
-  Validations vs invariants
- Always valid model
- makes illegal states unrepresentable
- 
