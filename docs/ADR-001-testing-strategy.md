# ADR-001: Testing Strategy for Omni Product Service

## Status
Accepted

## Context
We need to establish a realistic testing strategy for the Omni Product Spring Boot application that accommodates our current junior development team skill level while providing a path for growth. The team currently focuses on end-to-end testing only, and we need to balance this with practical learning progression.

## Current Reality Assessment

### Team Constraints
- **Junior developers** - Limited experience with testing frameworks
- **Learning curve** - Need to master Spring Boot, JPA, and testing concepts
- **Time pressure** - Focus on delivering features first
- **Complexity tolerance** - Prefer simple, understandable tests

### Current State
- **E2E testing only** - Using `@SpringBootTest` with `@AutoConfigureMockMvc`
- **No unit tests** - Business logic tested only through HTTP layer
- **No test isolation** - Full Spring context for every test
- **H2 database** - In-memory for development/testing

## Decision: Pragmatic E2E-First Approach

### Phase 1: Current State (Acceptable for Now)
```
E2E Tests (100%)
────────────────
Unit Tests (0%)
Integration Tests (0%)
```

**Rationale**: 
- Learning one testing approach is better than none
- E2E tests provide confidence that features work end-to-end
- Simpler to understand for junior developers
- Immediate feedback on feature delivery

### Phase 2: Gradual Evolution (Next 2-3 Sprints)
```
    E2E Tests (70%)
   ─────────────────
  Integration Tests (20%)
 ─────────────────────────
Unit Tests (10%)
```

**Progression Path**:
1. Add simple unit tests for utility methods
2. Introduce repository testing with `@DataJpaTest`
3. Keep E2E tests for complex workflows

### Phase 3: Mature Testing Pyramid (Future)
```
    E2E Tests (20%)
   ─────────────────
  Integration Tests (30%)
 ─────────────────────────
Unit Tests (50%)
```

## Current E2E Testing Strategy

### What Works Well for Juniors
✅ **Simple setup** - One test approach to learn
✅ **Real scenarios** - Tests match user workflows
✅ **Full confidence** - If E2E passes, feature works
✅ **Easy debugging** - Can step through entire flow

### Acceptable Limitations (For Now)
❌ **Slower execution** - 3-5 seconds per test
❌ **Less isolation** - Tests can affect each other
❌ **Harder debugging** - More layers to investigate
❌ **Resource intensive** - Full Spring context

### What is problematic

 - Integration vs. Unit Tests: The test is a @SpringBootTest, which loads the full application context. This is good for integration testing but slow. Consider adding @WebMvcTest for faster, isolated controller unit tests using mocks for the service.
 - Test Data Isolation: The test performs CRUD operations in sequence. While effective for a quick check, it makes tests interdependent. Individual tests should ideally be isolated (e.g., one test for Create, another for Find).


### Current Test Structure
```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCrudOperations() throws Exception {
        // Test complete user workflow
        // Create → Read → Update → Delete
    }
}
```

## MockMvc Analysis for Junior Developers

### Why MockMvc Works for Juniors
✅ **HTTP-like testing** - Familiar request/response pattern
✅ **JSON assertions** - Easy to understand JSON paths
✅ **Spring integration** - No manual setup required
✅ **Good documentation** - Plenty of examples available

### Limitations to Acknowledge
❌ **False confidence** - Tests pass but production may fail
❌ **Network layer missing** - No real HTTP behavior
❌ **Performance blind spots** - Don't test real response times

### Industry Criticisms (Context for Future)
**Note**: These are important but not blocking for junior teams:
- "Mock-heavy tests give false confidence" - True, but better than no tests
- "Over-mocking creates brittle tests" - Avoid by keeping mocks simple
- "Test complexity exceeds production code" - Monitor and refactor

## Practical Guidelines for Junior Team

### 1. Keep E2E Tests Simple
```java
@Test
void shouldCreateAndGetProduct() throws Exception {
    // 1. Create product
    mockMvc.perform(post("/api/products")
            .contentType(JSON)
            .content(productJson))
            .andExpect(status().isOk());
    
    // 2. Get product
    mockMvc.perform(get("/api/products/p1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Product"));
}
```

### 2. Test User Stories, Not Implementation
✅ **DO**: Test "user can create product and view it"
❌ **DON'T**: Test "controller calls service which calls repository"

### 3. Use Test Data Builders
```java
public class ProductTestData {
    public static Product validProduct() {
        return new Product(
            "p1", "Test Product", "test-product",
            new Product.Price(100.0, 20.0, 0.2),
            List.of("discount1"), 
            Map.of("main", "image-url"),
            1.5, "10x10x10", 100, 50,
            new Product.Warehouse("Main Warehouse")
        );
    }
}
```

### 4. Focus on Happy Paths First
- Test normal user workflows
- Add error handling tests later
- Don't worry about edge cases initially

## Learning Path to Better Testing

### Sprint 1-2: Master E2E Testing
- Understand MockMvc thoroughly
- Learn JSON assertions with JsonPath
- Practice test data management
- Get comfortable with H2 database testing

### Sprint 3-4: Add Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository repository;
    
    @InjectMocks
    private ProductService service;
    
    @Test
    void shouldSaveProduct() {
        // Simple business logic testing
    }
}
```

### Sprint 5-6: Repository Testing
```java
@DataJpaTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository repository;
    
    @Test
    void shouldFindBySlug() {
        // Database-specific testing
    }
}
```

### Sprint 7+: Test Pyramid Refinement
- Introduce test slices appropriately
- Add performance testing
- Consider contract testing

## Risk Mitigation for Junior Team

### 1. Test Quality Checklist
- [ ] Test has clear purpose (user story)
- [ ] Test data is realistic
- [ ] Assertions are meaningful
- [ ] Test is independent (can run alone)
- [ ] Test is fast enough (< 10 seconds)

### 2. Code Review Guidelines
- Review test logic as carefully as production code
- Ensure tests actually test what they claim
- Check for proper cleanup and isolation
- Verify test data is appropriate

### 3. Common Pitfalls to Avoid
```java
// ❌ Don't test implementation details
mockMvc.perform(get("/api/products"))
    .andExpect(jsonPath("$.serviceCalled").value(true));

// ✅ Test observable behavior
mockMvc.perform(get("/api/products"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[0].name").value("Expected Product"));
```

## Implementation Timeline

### Current Sprint (Accept E2E Only)
- Continue with `@SpringBootTest` + `@AutoConfigureMockMvc`
- Focus on test coverage of user stories
- Improve test data management
- Add basic error scenario testing

### Next 2-3 Sprints (Introduction to Unit Testing)
- Add simple unit tests for utility methods
- Introduce Mockito for service layer testing
- Keep E2E tests for complex workflows
- Measure test execution time improvements

### 3-6 Sprints (Build Testing Pyramid)
- Introduce `@WebMvcTest` for controller testing
- Add `@DataJpaTest` for repository testing
- Reduce E2E test count to critical paths only
- Establish performance benchmarks

## Success Metrics

### Short Term (This Sprint)
- [ ] All user stories have E2E tests
- [ ] Test execution time < 30 seconds total
- [ ] Zero test failures in CI/CD
- [ ] Team comfortable with MockMvc

### Medium Term (Next Quarter)
- [ ] 20% of tests are unit tests
- [ ] Test execution time reduced by 50%
- [ ] Team can write both unit and integration tests
- [ ] Test coverage > 80%

### Long Term (Next Year)
- [ ] Proper test pyramid (50/30/20)
- [ ] Test-driven development for new features
- [ ] Performance testing in CI/CD
- [ ] Team can mentor new developers on testing

## Consequences

### Positive (Immediate)
- **Lower learning curve** - One testing approach to master
- **Feature confidence** - E2E tests prove features work
- **Team morale** - Achievable goals, visible progress
- **User focus** - Tests aligned with business value

### Negative (Acceptable for Now)
- **Slower feedback** - E2E tests take longer
- **Resource usage** - More memory/CPU for tests
- **Limited isolation** - Tests may interfere
- **Technical debt** - Will need refactoring later

### Risks (Mitigated)
- **Test brittleness** → Use simple, stable test data
- **Slow CI/CD** → Accept for now, optimize later
- **Poor coverage** → Focus on critical user paths
- **Team burnout** → Gradual learning progression

## When to Evolve This Strategy

**Triggers for moving to next phase**:
1. Team comfortable with current E2E approach
2. CI/CD execution time becomes problematic
3. Team requests more advanced testing techniques
4. Production issues that E2E tests didn't catch
5. New team members with testing experience join

## References

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Testing for Junior Developers](https://martinfowler.com/articles/testing-strategies.html)

---

**Decision Date**: 2025-01-18  
**Author**: Development Team  
**Status**: Accepted (E2E-First Approach)  
**Review Date**: 2025-02-18  
**Next Review**: When team ready for Phase 2
