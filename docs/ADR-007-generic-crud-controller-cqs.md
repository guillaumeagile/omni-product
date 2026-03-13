# ADR-007: Generic CRUD Controller with Command Query Separation

## Status
Proposed

## Context

As the Omni Product application grows with multiple bounded contexts (Products, Suppliers, Stock, Catalog), we need a consistent pattern for managing basic CRUD operations across controllers. Currently, each controller implements CRUD logic independently, leading to:

- **Code duplication** - Similar patterns repeated across ProductController, SupplierController, etc.
- **Inconsistent error handling** - Each controller throws exceptions directly (violating the principle of "never throw exceptions in controllers")
- **Mixed responsibilities** - Controllers handle both command (Create, Update, Delete) and query (Read) operations without clear separation
- **Difficult to scale** - Adding new bounded contexts requires replicating the same controller patterns

Additionally, we want to introduce **Command Query Separation (CQS)** to:
- Clarify intent: Commands modify state, Queries retrieve state
- Enable independent optimization: Commands and Queries can be optimized differently
- Support future CQRS evolution: Separate read and write models if needed
- Improve testability: Commands and Queries can be tested in isolation

## Decision

We will implement a **generic CRUD controller pattern with Command Query Separation** using the following architecture:

### 1. Command and Query Abstraction

```java
// Base abstractions for CQS
public interface Command<R> {
    // Represents an operation that modifies state
    // R is the return type (usually a DTO or ID)
}

public interface Query<R> {
    // Represents an operation that retrieves state
    // R is the return type (usually a DTO or collection)
}

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

### 2. Generic CRUD Controller

```java
@RestController
@RequestMapping("/api/{resource}")
public class GenericCrudController<T, ID, CreateCmd extends Command<T>, 
                                   UpdateCmd extends Command<T>,
                                   GetQuery extends Query<T>,
                                   ListQuery extends Query<List<T>>> {
    
    private final CommandHandler<CreateCmd, T> createHandler;
    private final CommandHandler<UpdateCmd, T> updateHandler;
    private final CommandHandler<?, Void> deleteHandler;
    private final QueryHandler<GetQuery, T> getHandler;
    private final QueryHandler<ListQuery, List<T>> listHandler;
    
    // Constructor injection of handlers
    
    @PostMapping
    public ResponseEntity<T> create(@RequestBody CreateCmd command) {
        return ResponseEntity.ok(createHandler.handle(command));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<T> getById(@PathVariable ID id) {
        GetQuery query = new GetQuery(id);
        return ResponseEntity.ok(getHandler.handle(query));
    }
    
    @GetMapping
    public ResponseEntity<List<T>> getAll() {
        ListQuery query = new ListQuery();
        return ResponseEntity.ok(listHandler.handle(query));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<T> update(@PathVariable ID id, @RequestBody UpdateCmd command) {
        return ResponseEntity.ok(updateHandler.handle(command));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable ID id) {
        deleteHandler.handle(new DeleteCommand(id));
        return ResponseEntity.noContent().build();
    }
}
```

### 3. Example Implementation for Products

#### Commands
```java
// Create Product Command
public record CreateProductCommand(
    String name,
    String slug,
    Product.Price price,
    List<String> discounts,
    Map<String, String> images,
    Double weight,
    String dimensions,
    Integer quantity,
    Integer minStock,
    Product.Warehouse warehouse
) implements Command<ProductDTO> {}

// Update Product Command
public record UpdateProductCommand(
    String id,
    String name,
    String slug,
    Product.Price price,
    List<String> discounts,
    Map<String, String> images,
    Double weight,
    String dimensions,
    Integer quantity,
    Integer minStock,
    Product.Warehouse warehouse
) implements Command<ProductDTO> {}

// Delete Product Command
public record DeleteProductCommand(String id) implements Command<Void> {}
```

#### Queries
```java
// Get Product by ID Query
public record GetProductQuery(String id) implements Query<ProductDTO> {}

// List All Products Query
public record ListProductsQuery() implements Query<List<ProductDTO>> {}

// Advanced Query Example
public record FindProductsBySupplierQuery(String supplierId) implements Query<List<ProductDTO>> {}
```

#### Handlers
```java
@Component
public class CreateProductCommandHandler implements CommandHandler<CreateProductCommand, ProductDTO> {
    
    private final ProductService productService;
    private final ProductMapper mapper;
    
    @Override
    public ProductDTO handle(CreateProductCommand command) {
        Product product = productService.create(command);
        return mapper.toDTO(product);
    }
}

@Component
public class GetProductQueryHandler implements QueryHandler<GetProductQuery, ProductDTO> {
    
    private final ProductService productService;
    private final ProductMapper mapper;
    
    @Override
    public ProductDTO handle(GetProductQuery query) {
        Product product = productService.findById(query.id());
        return mapper.toDTO(product);
    }
}
```

### 4. Error Handling via Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("PRODUCT_NOT_FOUND", ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidProductException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProduct(InvalidProductException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_PRODUCT", ex.getMessage()));
    }
    
    // Other exception handlers...
}
```

### 5. Dependency Injection Configuration

```java
@Configuration
public class CrudHandlerConfiguration {
    
    @Bean
    public CommandHandler<CreateProductCommand, ProductDTO> createProductHandler(
            ProductService productService,
            ProductMapper mapper) {
        return new CreateProductCommandHandler(productService, mapper);
    }
    
    @Bean
    public QueryHandler<GetProductQuery, ProductDTO> getProductHandler(
            ProductService productService,
            ProductMapper mapper) {
        return new GetProductQueryHandler(productService, mapper);
    }
    
    // Other handler beans...
}
```

## Benefits

### Immediate
- **Consistency**: All CRUD operations follow the same pattern
- **No exceptions in controllers**: Errors handled centrally via `@ControllerAdvice`
- **Clear intent**: Commands vs Queries are explicitly separated
- **Reusability**: Handlers can be used from multiple contexts (HTTP, events, scheduled tasks)
- **Testability**: Each handler can be unit tested independently

### Future-Proof
- **CQRS evolution**: Can split read/write models without refactoring controllers
- **Event sourcing**: Commands naturally map to domain events
- **Async operations**: Commands can be made async via message queues
- **Caching strategy**: Queries can be cached independently
- **Performance optimization**: Read and write paths can be optimized separately

## Tradeoffs

### Complexity
- **More classes**: Each CRUD operation requires Command/Query + Handler classes
- **Learning curve**: Team needs to understand CQS pattern
- **Boilerplate**: Initial setup requires more code than traditional controllers

### Mitigation
- Use code generation or templates for repetitive handler creation
- Provide clear examples and documentation
- Start with one bounded context (Products) as a pilot
- Gradually migrate other controllers

## Implementation Strategy

### Phase 1: Pilot (Current Sprint)
- Implement generic CRUD pattern for ProductController
- Create Command/Query/Handler classes for Product CRUD
- Set up global exception handler
- Document pattern with examples

### Phase 2: Expansion (Next Sprint)
- Migrate SupplierController to new pattern
- Refine handler configuration based on learnings
- Add advanced queries (filtering, pagination)

### Phase 3: Evolution (Future)
- Consider CQRS if read/write patterns diverge significantly
- Implement event publishing from commands
- Add async command handling via message queue

## Consequences

### Positive
- **Maintainability**: Clear separation of concerns
- **Scalability**: Easy to add new bounded contexts
- **Consistency**: Uniform error handling and response patterns
- **Testability**: Handlers are pure, easy to test
- **Future-ready**: Foundation for CQRS if needed

### Negative
- **Initial overhead**: More code to write and maintain
- **Team adjustment**: Learning curve for CQS pattern
- **Potential over-engineering**: For simple CRUD, might be overkill

## Alternatives Considered

### 1. Traditional Service Layer (Current State)
- **Pros**: Simpler, less boilerplate
- **Cons**: Controllers still throw exceptions, mixed responsibilities, harder to evolve

### 2. Spring Data REST
- **Pros**: Automatic CRUD endpoints
- **Cons**: Limited customization, exposes entities directly, no error handling control

### 3. Lightweight CQS (Commands only, no Queries)
- **Pros**: Less boilerplate than full CQS
- **Cons**: Queries remain implicit, harder to optimize read paths

## References

- [Command Query Separation (CQS)](https://martinfowler.com/bliki/CommandQuerySeparation.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Spring Boot Best Practices](https://spring.io/guides/gs/rest-service/)
- [Domain-Driven Design - Commands and Events](https://vaughnvernon.com/domain-driven-design-distilled/)

---

**Decision Date**: 2025-03-13  
**Author**: Development Team  
**Status**: Proposed  
**Review Date**: 2025-03-20  
**Next Steps**: Implement pilot with ProductController
