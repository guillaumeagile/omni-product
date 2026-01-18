# Java 25 Modern Features - Comprehensive Guide

This document summarizes the key Java 25 concepts demonstrated in the test suite.

## Table of Contents
1. [Records](#records)
2. [Pattern Matching](#pattern-matching)
3. [Sealed Types](#sealed-types)
4. [Functional Programming with Vavr](#functional-programming-with-vavr)
5. [Testing Framework](#testing-framework)

---

## Records

Records are immutable data carriers that reduce boilerplate code. They automatically generate `equals()`, `hashCode()`, `toString()`, and accessor methods.

### Basic Record Definition
```java
record Employee(String id, String name, Department department) {}
record Department(String name, String budget) {}
```

### Key Benefits
- **Immutability**: All fields are final
- **Conciseness**: No need for getters, constructors, or equals/hashCode
- **Transparency**: Clear intent that this is a data container
- **Nesting**: Records can contain other records for complex data structures

### Example Usage
```java
Employee emp = new Employee("EMP001", "John Doe", new Department("Engineering", "500000"));
```

---

## Pattern Matching

Pattern matching enables more expressive and safer code by destructuring data structures directly in control flow.

### 1. Record Pattern Destructuring

#### Simple Destructuring
Extract fields from a record using `instanceof`:
```java
if (employee instanceof Employee(String id, String name, _)) {
    // id and name are now available
}
```

#### Nested Destructuring
Destructure nested records in a single pattern:
```java
if (employee instanceof Employee(String id, String name, Department(String deptName, String budget))) {
    // All fields are extracted and available
}
```

### 2. Pattern Matching in Switch Expressions

#### Basic Switch with Patterns
```java
String level = switch (employee) {
    case Employee(String id, String name, Department dept) when dept.name().equals("Engineering") 
        -> "Senior - Engineering";
    case Employee(String id, String name, Department dept) when dept.name().equals("Sales") 
        -> "Mid-level - Sales";
    default -> "Unknown";
};
```

#### Guard Conditions
Use `when` clauses to add additional conditions:
```java
case Person(String name, int age, Address(String city, _, _))
    when age >= 18 && city.equals("Boston") -> true;
```

#### Complex Nested Patterns
```java
case Order(String id, Customer(String name, String email), Item(String product, double price, int qty)) 
    when qty > 2 -> {
        double totalPrice = price * qty * 0.9;  // 10% discount
        yield new ProcessedOrder(id, name, product, qty, price, totalPrice, true);
    }
```

### 3. Wildcards in Patterns

Use `_` to ignore fields you don't need:
```java
if (address instanceof Address(String street, _, String zip)) {
    // Only street and zip are extracted
}
```

### 4. If-Else Pattern Matching

Combine pattern matching with traditional if-else chains:
```java
if (employee instanceof Employee(String id, String name, Department dept) 
    && dept.name().equals("Engineering")) {
    status = "Senior Engineer";
} else if (employee instanceof Employee(String id2, String name2, Department dept2) 
    && dept2.name().equals("Sales")) {
    status = "Sales Rep";
}
```

---

## Sealed Types

Sealed types restrict which classes can extend or implement a type, enabling exhaustive pattern matching.

### Sealed Interface Definition
```java
sealed interface Shape permits Circle, Rectangle, Triangle {}

record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double a, double b, double c) implements Shape {}
```

### Benefits
- **Exhaustiveness**: Compiler ensures all permitted types are handled
- **Safety**: Prevents unexpected subclasses
- **Pattern Matching**: Enables complete pattern coverage without default cases

### Example with Pattern Matching
```java
String info = switch (shape) {
    case Circle(double r) -> "Circle with radius " + r;
    case Rectangle(double w, double h) -> "Rectangle " + w + " x " + h;
    case Triangle(double a, double b, double c) -> "Triangle";
};
```

---

## Functional Programming with Vavr

Vavr provides immutable collections and functional abstractions for a more functional Java style.

### 1. Option Type

`Option` represents an optional value that may or may not be present:

```java
sealed interface Result<T> permits Success, Failure {}
record Success<T>(T value) implements Result<T> {}
record Failure<T>(String error) implements Result<T> {}

Option<String> result = validatePayment(payment)
    .flatMap(this::processPayment);

if (result.isDefined()) {
    String value = result.get();
}
```

### 2. Try Type

`Try` captures the result of an operation that may throw an exception:

```java
Try<Integer> result = Try.of(() -> Integer.parseInt("123"));
result.onSuccess(value -> System.out.println(value));
result.onFailure(error -> System.out.println(error));
```

### 3. Immutable Collections

Vavr provides immutable versions of common collections:

```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5);
List<Integer> doubled = numbers.map(n -> n * 2);
List<Integer> evens = numbers.filter(n -> n % 2 == 0);
```

### 4. Functional Composition

Combine functions for powerful data transformations:

```java
List<Integer> numbers = List.of(2, 4);
List<Result<Integer>> results = numbers
    .map(this::divideByTwo)
    .map(this::multiplyByThree);

List<Integer> successValues = results
    .filter(r -> r instanceof Success)
    .map(r -> ((Success<Integer>) r).value());
```

---

## Testing Framework

### JUnit 5 (Jupiter)

Modern testing framework with powerful features:

```java
@DisplayName("Employee Record Patterns - Java 25")
class EmployeeRecordPatternTest {
    
    @Test
    @DisplayName("should destructure nested employee and department records")
    void testNestedRecordDestructuring() {
        // Test implementation
    }
}
```

#### Key Features
- **@DisplayName**: Human-readable test names
- **@Test**: Marks test methods
- **Parameterized Tests**: Run the same test with different inputs
- **Nested Test Classes**: Organize related tests

### AssertJ

Fluent assertion library for expressive test assertions:

```java
assertThat(employee.id()).isEqualTo("EMP001");
assertThat(employee.name()).isEqualTo("John Doe");
assertThat(totalPrice).isCloseTo(134.96, org.assertj.core.data.Offset.offset(0.01));
assertThat(isValid).isTrue();
```

#### Common Assertions
- `isEqualTo()` - Equality check
- `isCloseTo()` - Floating-point comparison with tolerance
- `isTrue()` / `isFalse()` - Boolean checks
- `contains()` - String/collection contains
- `isBlank()` - String validation

---

## Practical Examples

### Employee Classification
Demonstrates nested record destructuring and pattern matching with guards:
```java
Employee emp = new Employee("EMP001", "John", new Department("Engineering", "500000"));
String level = classifyEmployee(emp);  // "Senior - Engineering"
```

### Voting Eligibility
Shows complex guard conditions with multiple criteria:
```java
Person person = new Person("Alice", 25, new Address("456 Oak Ave", "Boston", "02101"));
boolean canVote = canVote(person);  // true (age >= 18 AND city == "Boston")
```

### Order Processing
Demonstrates structured return types with conditional logic:
```java
Order order = new Order("ORD-002", new Customer("Jane", "jane@example.com"), 
                       new Item("Mouse", 29.99, 5));
ProcessedOrder processed = processOrder(order);
// Returns: ProcessedOrder with 10% discount (qty > 2)
```

---

## Best Practices

### 1. Use Records for Data Classes
- Replace POJOs with records for cleaner code
- Leverage immutability for thread safety

### 2. Pattern Matching for Control Flow
- Use `instanceof` patterns instead of casts
- Leverage `when` guards for complex conditions
- Prefer switch expressions over if-else chains

### 3. Sealed Types for Domain Models
- Restrict inheritance hierarchies
- Enable exhaustive pattern matching
- Document valid subtypes explicitly

### 4. Functional Programming
- Use `Option` for nullable values
- Use `Try` for exception handling
- Chain operations with `map()`, `flatMap()`, `filter()`

### 5. Testing
- Write descriptive test names with `@DisplayName`
- Use AssertJ for fluent assertions
- Test both happy paths and edge cases

---

## Summary

Java 25 brings powerful features for writing cleaner, more expressive code:

| Feature | Purpose | Benefit |
|---------|---------|---------|
| **Records** | Immutable data carriers | Less boilerplate, clearer intent |
| **Pattern Matching** | Destructure data in control flow | More readable, safer code |
| **Sealed Types** | Restrict class hierarchies | Exhaustive pattern matching |
| **Vavr** | Functional programming library | Immutability, better error handling |
| **JUnit 5** | Modern testing framework | Powerful, expressive tests |
| **AssertJ** | Fluent assertions | Readable test assertions |

These features work together to enable modern, functional-style Java programming with strong type safety and minimal boilerplate.
