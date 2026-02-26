# ADR-004: JPA Entity Model for Product

## Status
Accepted

## Context
The Product model needed to be converted from a Java record to a JPA entity to support database persistence with Spring Data JPA.

## Decision
Convert Product from record to JPA entity class with embedded value objects.

## Implementation

### Entity Structure
```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    private String id;
    
    @Embedded
    private Price price;
    
    @ElementCollection
    private List<String> discounts;
    
    @Embedded
    private Warehouse warehouse;
}
```

### Value Objects
- **Price** - @Embeddable with base, tax, taxRate
- **Warehouse** - @Embeddable with location
- **Images** - Simplified to Map<String, String>

## Rationale

### Why JPA Entity
✅ **Spring Data Integration** - Automatic CRUD
✅ **Database Mapping** - Automatic schema generation
✅ **Relationship Support** - Future extensibility
✅ **Transaction Support** - ACID guarantees

### Why Not Record
❌ **No JPA Support** - Records not persistable
❌ **Immutability Issues** - JPA requires mutability
❌ **Limited Features** - No lazy loading, etc.

## Consequences

### Positive
- **Full ORM Support** - All JPA features available
- **Automatic Persistence** - Repository handles CRUD
- **Schema Management** - Hibernate DDL auto-creation
- **Future-proof** - Supports complex relationships

### Negative
- **Boilerplate** - Getters/setters required
- **Mutability** - Less functional style
- **Complexity** - More code than record
- **Performance** - Lazy loading overhead

### Simplifications Made
- **Images**: Map<String, String> instead of complex ImageDetail
- **No Relationships**: No foreign keys initially
- **Embedded Only**: No separate tables for value objects

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted
