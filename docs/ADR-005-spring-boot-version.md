# ADR-005: Spring Boot Version Selection

## Status
Accepted

## Context
The project needed a stable, modern Spring Boot version that provides good support for Java 21 and includes all necessary features for REST API development.

## Decision
Use Spring Boot 3.4.1 with Spring Framework 6.2.1.

## Rationale

### Why Spring Boot 3.4.1
✅ **Latest Stable** - Current production release
✅ **Java 21 Support** - Full compatibility
✅ **Spring 6.2** - Modern framework features
✅ **Long Support** - Supported until 2026
✅ **Native Compilation** - GraalVM support available

### Why Not Spring Boot 2.x
❌ **Older Framework** - Spring 5.x is outdated
❌ **Java 8 Focused** - Not optimized for Java 21
❌ **End of Life** - Support ending soon

## Dependencies

### Core
- spring-boot-starter-web
- spring-boot-starter-data-jpa

### Database
- h2 (runtime)

### Testing
- spring-boot-starter-test
- junit-platform-launcher
- assertj-core

## Consequences

### Positive
- **Modern Features** - Latest Spring capabilities
- **Good Documentation** - Well-supported version
- **Performance** - Latest optimizations
- **Security** - Regular updates

### Negative
- **Breaking Changes** - From Spring 2.x
- **Learning Curve** - New features to learn
- **Dependency Updates** - Regular maintenance needed

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted
