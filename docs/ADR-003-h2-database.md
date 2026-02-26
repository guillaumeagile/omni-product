# ADR-003: H2 Database for Development and Testing

## Status
Accepted

## Context
The application needed a lightweight, in-memory database for development and testing that requires minimal configuration and provides fast feedback loops.

## Decision
Use H2 as the default database for development and testing with Spring Data JPA.

## Configuration
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

## Rationale

### Why H2
✅ **Zero Configuration** - Works out of the box
✅ **In-Memory** - Fast test execution
✅ **Spring Boot Native** - Auto-configured
✅ **SQL Compatible** - Standard SQL support
✅ **Console Available** - `/h2-console` for debugging

### Why Not PostgreSQL/MySQL
❌ **Requires Setup** - External service needed
❌ **Slower Tests** - Network overhead
❌ **Overkill for Dev** - Too much for junior team

## Consequences

### Positive
- **Fast feedback** - In-memory operations
- **No external dependencies** - Self-contained
- **Easy debugging** - H2 console available
- **Junior-friendly** - Simple to understand

### Negative
- **Not production-ready** - Different from production DB
- **Limited features** - Some SQL dialects not supported
- **Data loss** - In-memory, no persistence

### Migration Path
- **Dev**: H2 in-memory
- **CI/CD**: TestContainers with PostgreSQL
- **Production**: PostgreSQL with migrations

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted
