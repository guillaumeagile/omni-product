# ADR-006: Package Naming Convention

## Status
Accepted

## Context
The project initially had inconsistent package naming with nested structures like `com.example.controller.model` and `com.example.controller.service`. This violated Java package naming standards and made the codebase harder to navigate.

## Decision
Use standard Java package naming convention: `com.omniproduct.<layer>` where layer is `controller`, `service`, `repository`, or `model`.

## Structure
```
com.omniproduct
├── controller/     - REST endpoints
├── service/        - Business logic
├── repository/     - Data access
└── model/          - Domain entities
```

## Rationale

### Why This Structure
✅ **Standard Convention** - Follows Java best practices
✅ **Clear Separation** - Each layer has its own package
✅ **Easy Navigation** - Developers know where to find code
✅ **Scalability** - Easy to add new features
✅ **IDE Support** - Better autocomplete and refactoring

### Why Not Nested (com.example.controller.model)
❌ **Non-standard** - Violates Java conventions
❌ **Deep Nesting** - Harder to navigate
❌ **Confusing** - Unclear layer separation
❌ **Poor IDE Support** - Longer import paths

## Consequences

### Positive
- **Standard Compliance** - Follows Java conventions
- **Better Organization** - Clear layer separation
- **Easier Refactoring** - Standard structure
- **Team Alignment** - Everyone knows the structure

### Negative
- **Refactoring Work** - Moving existing code
- **Import Updates** - All imports need updating
- **Short-term Disruption** - Tests may break temporarily

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted
