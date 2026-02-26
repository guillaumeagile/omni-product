# Lazy Loading Issue - Problem & Solution

## Problem Statement

When running the `shouldGetSuppliersByProduct()` test, the following error occurred:

```
java.lang.AssertionError: 
Expected size: 1 but was: 0 in:
[]
```

### Root Cause

The issue stemmed from Hibernate's lazy loading mechanism combined with the bidirectional relationship between `Supplier` and `Product`:

1. **Bidirectional Relationship**: 
   - `Supplier` has a `@OneToMany` relationship to `Product` with `cascade = CascadeType.ALL`
   - `Product` has a `@ManyToOne` relationship back to `Supplier`

2. **Lazy Loading Default**: 
   - The `products` collection in `Supplier` is lazy-loaded by default
   - When accessed outside a transaction, Hibernate cannot initialize the collection because the session is closed

3. **Method Implementation**:
   ```java
   public List<Supplier> getSuppliersByProduct(String productId) {
       return supplierRepository.findAll().stream()
           .filter(s -> s.getProducts() != null && s.getProducts().stream()
               .anyMatch(p -> p.getId().equals(productId)))
           .toList();
   }
   ```
   This method tries to access `s.getProducts()` (the lazy-loaded collection) without an active Hibernate session.

## Solution

### Step 1: Add `@Transactional` to Service Method

Added `@Transactional(readOnly = true)` annotation to the `getSuppliersByProduct()` method in `SupplierService`:

```java
@Transactional(readOnly = true)
public List<Supplier> getSuppliersByProduct(String productId) {
    productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    return supplierRepository.findAll().stream()
        .filter(s -> s.getProducts() != null && s.getProducts().stream()
            .anyMatch(p -> p.getId().equals(productId)))
        .toList();
}
```

**Why this works:**
- `@Transactional` keeps the Hibernate session open during method execution
- `readOnly = true` optimizes the transaction for read-only operations
- The lazy-loaded `products` collection can now be initialized within the active session

### Step 2: Add `@Transactional` to Test Method

Added `@Transactional` annotation to the test method:

```java
@Test
@Transactional
void shouldGetSuppliersByProduct() {
    // test code...
}
```

**Why this is needed:**
- Ensures the session remains active during the entire test execution
- Allows assertions to access lazy-loaded collections if needed
- Provides consistency with the service layer transaction handling

## Key Learnings

### Lazy Loading Pitfalls
1. **Session Closure**: Lazy-loaded collections cannot be accessed after the session closes
2. **Stream Operations**: Accessing collections in streams requires an active session
3. **Testing Context**: Tests need explicit transaction management for lazy-loaded data

### Best Practices Applied
1. **Service Layer Transactions**: Use `@Transactional` on service methods that access lazy-loaded data
2. **Read-Only Optimization**: Use `readOnly = true` for query-only operations
3. **Test Transactions**: Apply `@Transactional` to tests that verify lazy-loaded relationships

### Alternative Solutions (Not Used)
1. **Eager Loading**: Change `@OneToMany` to use `fetch = FetchType.EAGER` (performance impact)
2. **Query-Based Approach**: Use database queries instead of filtering in-memory (more complex)
3. **DTOs**: Convert to DTOs to avoid lazy loading issues (architectural change)

## Files Modified

- `src/main/java/com/omniproduct/service/SupplierService.java`
  - Added `@Transactional` import
  - Added `@Transactional(readOnly = true)` to `getSuppliersByProduct()`

- `src/test/java/com/omniproduct/service/SupplierServiceTest.java`
  - Added `@Transactional` to `shouldGetSuppliersByProduct()` test

## Result

✅ Test now passes successfully
✅ Lazy loading properly handled within transaction context
✅ No performance degradation (read-only optimization applied)
