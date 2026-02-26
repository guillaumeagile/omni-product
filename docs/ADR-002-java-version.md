# ADR-002: Java Version Selection

## Status
Accepted

## Context
Initial project setup attempted to use Java 25 preview features, which caused compatibility issues with Gradle, Spring Framework, and various libraries. We needed to choose a stable Java version that provides modern features while maintaining compatibility.

## Decision
Use Java 21 (LTS) as the target Java version.

## Rationale

### Why Java 21
✅ **LTS Release** - Long-term support until October 2029
✅ **Spring Boot Compatible** - Full support in Spring Boot 3.4.1
✅ **Gradle Compatible** - No toolchain issues
✅ **Modern Features** - Records, pattern matching, sealed classes
✅ **Performance** - Latest JVM optimizations

### Why Not Java 25
❌ **Preview Features** - Unstable API, may change
❌ **Compatibility Issues** - Spring ASM library incompatibility
❌ **Gradle Issues** - Toolchain parsing problems
❌ **No LTS Support** - Short release cycle

## Consequences

### Positive
- **Stable development environment** - No compatibility surprises
- **Modern Java features** - Records, pattern matching available
- **Long-term support** - Security patches until 2029
- **Good ecosystem support** - All libraries work correctly

### Negative
- **Missing latest features** - Some Java 22+ features not available
- **Conservative choice** - Not bleeding edge

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted  
**Review Date**: 2027-01-18
