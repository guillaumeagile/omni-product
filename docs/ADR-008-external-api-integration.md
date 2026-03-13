# ADR-008: External API Integration with Clean Architecture

## Status
Proposed

## Context

The Omni Product application needs to integrate with external APIs to enrich domain functionality. Specifically, we need to call the **Impact CO2 API** (https://impactco2.fr/doc/api) to calculate transport-related carbon footprint metrics.

Current challenges:
- **No abstraction layer** - Direct HTTP calls from services would couple business logic to external APIs
- **Testability** - Hard to test services without mocking HTTP calls
- **Maintainability** - API changes would require changes throughout the codebase
- **Resilience** - No error handling or fallback strategies for API failures
- **Clean architecture violation** - External dependencies should not leak into domain logic

We need to:
1. Create a **port (interface)** representing the external API contract
2. Implement an **adapter** that calls the actual Impact CO2 API
3. Use **dependency injection** to inject the adapter into services
4. Handle errors gracefully without exposing HTTP details to domain logic
5. Keep the domain layer independent of external API implementation details

## Decision

We will implement **Hexagonal Architecture (Ports & Adapters)** for external API integration:

### 1. Port Definition (Domain Layer)

```java
// src/main/java/com/omniproduct/domain/port/TransportCarbonPort.java
package com.omniproduct.domain.port;

import com.omniproduct.domain.model.TransportCarbonMetrics;

/**
 * Port for calculating transport carbon footprint.
 * Abstracts the external Impact CO2 API from domain logic.
 */
public interface TransportCarbonPort {
    
    /**
     * Calculate carbon emissions for a transport scenario.
     * 
     * @param transportRequest Details about the transport (distance, mode, weight)
     * @return Carbon metrics including CO2 equivalent
     * @throws TransportCarbonException if calculation fails
     */
    TransportCarbonMetrics calculateEmissions(TransportRequest transportRequest);
    
    /**
     * Get supported transport modes.
     * 
     * @return List of available transport modes
     */
    List<String> getSupportedModes();
}
```

### 2. Domain Model for Transport Carbon

```java
// src/main/java/com/omniproduct/domain/model/TransportCarbonMetrics.java
package com.omniproduct.domain.model;

/**
 * Value object representing carbon emissions from transport.
 * Always valid - created only with valid data.
 */
public record TransportCarbonMetrics(
    Double co2EquivalentKg,           // Total CO2 equivalent in kilograms
    Double co2DirectKg,               // Direct CO2 emissions
    Double ch4EquivalentKg,           // Methane equivalent
    Double n2oEquivalentKg,           // Nitrous oxide equivalent
    String transportMode,             // Mode used (truck, train, plane, etc.)
    Double distance,                  // Distance in kilometers
    Double weight                     // Weight in kilograms
) {
    public TransportCarbonMetrics {
        if (co2EquivalentKg < 0 || distance < 0 || weight < 0) {
            throw new IllegalArgumentException("Carbon metrics cannot be negative");
        }
    }
}

// src/main/java/com/omniproduct/domain/model/TransportRequest.java
package com.omniproduct.domain.model;

/**
 * Request for carbon calculation.
 * Encapsulates transport parameters.
 */
public record TransportRequest(
    String transportMode,    // truck, train, plane, ship, etc.
    Double distanceKm,       // Distance in kilometers
    Double weightKg,         // Weight in kilograms
    String origin,           // Optional: origin location
    String destination       // Optional: destination location
) {
    public TransportRequest {
        if (transportMode == null || transportMode.isBlank()) {
            throw new IllegalArgumentException("Transport mode is required");
        }
        if (distanceKm == null || distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (weightKg == null || weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
    }
}
```

### 3. Domain Exception

```java
// src/main/java/com/omniproduct/domain/exception/TransportCarbonException.java
package com.omniproduct.domain.exception;

/**
 * Thrown when carbon calculation fails.
 * Does not expose HTTP or API details.
 */
public class TransportCarbonException extends RuntimeException {
    
    public TransportCarbonException(String message) {
        super(message);
    }
    
    public TransportCarbonException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4. Adapter Implementation (Infrastructure Layer)

```java
// src/main/java/com/omniproduct/infrastructure/adapter/ImpactCo2TransportAdapter.java
package com.omniproduct.infrastructure.adapter;

import com.omniproduct.domain.port.TransportCarbonPort;
import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import com.omniproduct.domain.exception.TransportCarbonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.List;

/**
 * Adapter for Impact CO2 API (https://impactco2.fr/doc/api).
 * Implements TransportCarbonPort to calculate transport emissions.
 */
@Component
public class ImpactCo2TransportAdapter implements TransportCarbonPort {
    
    private static final String BASE_URL = "https://impactco2.fr/api/v1";
    private static final String TRANSPORT_ENDPOINT = "/transport/emissions";
    
    private final RestTemplate restTemplate;
    private final ImpactCo2ApiClient apiClient;
    
    public ImpactCo2TransportAdapter(
            RestTemplate restTemplate,
            ImpactCo2ApiClient apiClient) {
        this.restTemplate = restTemplate;
        this.apiClient = apiClient;
    }
    
    @Override
    public TransportCarbonMetrics calculateEmissions(TransportRequest transportRequest) {
        try {
            ImpactCo2Request apiRequest = mapToApiRequest(transportRequest);
            ImpactCo2Response apiResponse = apiClient.calculateEmissions(apiRequest);
            return mapToDomainModel(apiResponse);
        } catch (RestClientException e) {
            throw new TransportCarbonException(
                "Failed to calculate transport emissions: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TransportCarbonException(
                "Unexpected error during carbon calculation: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getSupportedModes() {
        try {
            return apiClient.getSupportedTransportModes();
        } catch (RestClientException e) {
            throw new TransportCarbonException(
                "Failed to fetch supported transport modes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Map domain model to API request format.
     */
    private ImpactCo2Request mapToApiRequest(TransportRequest transportRequest) {
        return new ImpactCo2Request(
            transportRequest.transportMode(),
            transportRequest.distanceKm(),
            transportRequest.weightKg(),
            transportRequest.origin(),
            transportRequest.destination()
        );
    }
    
    /**
     * Map API response to domain model.
     */
    private TransportCarbonMetrics mapToDomainModel(ImpactCo2Response response) {
        return new TransportCarbonMetrics(
            response.co2Equivalent(),
            response.co2Direct(),
            response.ch4Equivalent(),
            response.n2oEquivalent(),
            response.transportMode(),
            response.distance(),
            response.weight()
        );
    }
}
```

### 5. API Client (HTTP Communication)

```java
// src/main/java/com/omniproduct/infrastructure/adapter/ImpactCo2ApiClient.java
package com.omniproduct.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

/**
 * Low-level HTTP client for Impact CO2 API.
 * Handles REST communication details.
 */
@Component
public class ImpactCo2ApiClient {
    
    private static final String BASE_URL = "https://impactco2.fr/api/v1";
    private static final String TRANSPORT_ENDPOINT = "/transport/emissions";
    private static final String MODES_ENDPOINT = "/transport/modes";
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    
    public ImpactCo2ApiClient(
            RestTemplate restTemplate,
            @Value("${impactco2.api.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }
    
    /**
     * Call Impact CO2 API to calculate emissions.
     */
    public ImpactCo2Response calculateEmissions(ImpactCo2Request request) {
        String url = BASE_URL + TRANSPORT_ENDPOINT;
        
        // Add authentication header if API key is configured
        var headers = new org.springframework.http.HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        headers.set("Content-Type", "application/json");
        
        var httpEntity = new org.springframework.http.HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(
            url,
            httpEntity,
            ImpactCo2Response.class
        );
    }
    
    /**
     * Fetch supported transport modes from API.
     */
    public List<String> getSupportedTransportModes() {
        String url = BASE_URL + MODES_ENDPOINT;
        
        var headers = new org.springframework.http.HttpHeaders();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        
        var httpEntity = new org.springframework.http.HttpEntity<>(headers);
        
        var response = restTemplate.getForObject(url, String[].class);
        return response != null ? List.of(response) : List.of();
    }
}
```

### 6. API DTOs (Infrastructure Layer)

```java
// src/main/java/com/omniproduct/infrastructure/adapter/ImpactCo2Request.java
package com.omniproduct.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for Impact CO2 API.
 * Matches API contract.
 */
public record ImpactCo2Request(
    @JsonProperty("transport_mode")
    String transportMode,
    
    @JsonProperty("distance_km")
    Double distanceKm,
    
    @JsonProperty("weight_kg")
    Double weightKg,
    
    @JsonProperty("origin")
    String origin,
    
    @JsonProperty("destination")
    String destination
) {}

// src/main/java/com/omniproduct/infrastructure/adapter/ImpactCo2Response.java
package com.omniproduct.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from Impact CO2 API.
 * Matches API contract.
 */
public record ImpactCo2Response(
    @JsonProperty("co2_equivalent_kg")
    Double co2Equivalent,
    
    @JsonProperty("co2_direct_kg")
    Double co2Direct,
    
    @JsonProperty("ch4_equivalent_kg")
    Double ch4Equivalent,
    
    @JsonProperty("n2o_equivalent_kg")
    Double n2oEquivalent,
    
    @JsonProperty("transport_mode")
    String transportMode,
    
    @JsonProperty("distance_km")
    Double distance,
    
    @JsonProperty("weight_kg")
    Double weight
) {}
```

### 7. Service Using the Port

```java
// src/main/java/com/omniproduct/service/ProductTransportService.java
package com.omniproduct.service;

import com.omniproduct.domain.port.TransportCarbonPort;
import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import org.springframework.stereotype.Service;

/**
 * Service that uses TransportCarbonPort.
 * Depends on abstraction, not implementation.
 */
@Service
public class ProductTransportService {
    
    private final TransportCarbonPort transportCarbonPort;
    
    public ProductTransportService(TransportCarbonPort transportCarbonPort) {
        this.transportCarbonPort = transportCarbonPort;
    }
    
    /**
     * Calculate carbon footprint for product shipment.
     */
    public TransportCarbonMetrics calculateShipmentCarbon(
            String transportMode,
            Double distanceKm,
            Double weightKg) {
        
        TransportRequest request = new TransportRequest(
            transportMode,
            distanceKm,
            weightKg,
            null,
            null
        );
        
        return transportCarbonPort.calculateEmissions(request);
    }
}
```

### 8. Dependency Injection Configuration

```java
// src/main/java/com/omniproduct/config/ExternalApiConfiguration.java
package com.omniproduct.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

/**
 * Configuration for external API integrations.
 */
@Configuration
public class ExternalApiConfiguration {
    
    /**
     * RestTemplate bean with sensible defaults for external API calls.
     */
    @Bean
    public RestTemplate externalApiRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

### 9. Application Properties

```properties
# application.properties
impactco2.api.key=${IMPACT_CO2_API_KEY:}
impactco2.api.base-url=https://impactco2.fr/api/v1
impactco2.api.timeout.connect=5000
impactco2.api.timeout.read=10000
```

### 10. Unit Test Example

```java
// src/test/java/com/omniproduct/infrastructure/adapter/ImpactCo2TransportAdapterTest.java
package com.omniproduct.infrastructure.adapter;

import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import com.omniproduct.domain.exception.TransportCarbonException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImpactCo2TransportAdapterTest {
    
    @Mock
    private ImpactCo2ApiClient apiClient;
    
    @InjectMocks
    private ImpactCo2TransportAdapter adapter;
    
    @Test
    void shouldCalculateEmissions() {
        // Arrange
        TransportRequest request = new TransportRequest("truck", 100.0, 500.0, null, null);
        ImpactCo2Response apiResponse = new ImpactCo2Response(
            250.0, 200.0, 25.0, 5.0, "truck", 100.0, 500.0
        );
        when(apiClient.calculateEmissions(any())).thenReturn(apiResponse);
        
        // Act
        TransportCarbonMetrics result = adapter.calculateEmissions(request);
        
        // Assert
        assertThat(result.co2EquivalentKg()).isEqualTo(250.0);
        assertThat(result.transportMode()).isEqualTo("truck");
    }
    
    @Test
    void shouldThrowDomainExceptionOnApiFailure() {
        // Arrange
        TransportRequest request = new TransportRequest("truck", 100.0, 500.0, null, null);
        when(apiClient.calculateEmissions(any()))
            .thenThrow(new org.springframework.web.client.RestClientException("API error"));
        
        // Act & Assert
        assertThatThrownBy(() -> adapter.calculateEmissions(request))
            .isInstanceOf(TransportCarbonException.class)
            .hasMessageContaining("Failed to calculate transport emissions");
    }
}
```

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                             │
│  (Business Logic - Independent of External APIs)           │
├─────────────────────────────────────────────────────────────┤
│  • TransportCarbonPort (Interface)                          │
│  • TransportRequest (Value Object)                          │
│  • TransportCarbonMetrics (Value Object)                    │
│  • TransportCarbonException (Domain Exception)              │
└────────────────────────┬────────────────────────────────────┘
                         │ depends on abstraction
                         │
┌────────────────────────▼────────────────────────────────────┐
│              Application/Service Layer                       │
├─────────────────────────────────────────────────────────────┤
│  • ProductTransportService                                  │
│    (uses TransportCarbonPort via DI)                        │
└────────────────────────┬────────────────────────────────────┘
                         │ depends on abstraction
                         │
┌────────────────────────▼────────────────────────────────────┐
│           Infrastructure/Adapter Layer                       │
│  (External API Integration - Implementation Details)        │
├─────────────────────────────────────────────────────────────┤
│  • ImpactCo2TransportAdapter (implements Port)              │
│  • ImpactCo2ApiClient (HTTP communication)                  │
│  • ImpactCo2Request/Response (API DTOs)                     │
│  • RestTemplate (Spring HTTP client)                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
            ┌────────────────────────────┐
            │  Impact CO2 API            │
            │ (https://impactco2.fr)     │
            └────────────────────────────┘
```

## Benefits

### Clean Architecture
- **Dependency inversion**: Domain depends on abstraction (Port), not concrete implementation
- **Testability**: Easy to mock TransportCarbonPort for unit tests
- **Flexibility**: Can swap ImpactCo2TransportAdapter for another implementation without changing domain logic

### Separation of Concerns
- **Domain layer**: Pure business logic, no HTTP details
- **Infrastructure layer**: API communication, mapping, error handling
- **Service layer**: Orchestrates domain logic and ports

### Resilience
- **Error handling**: API failures mapped to domain exceptions
- **Timeout configuration**: RestTemplate configured with sensible defaults
- **Graceful degradation**: Services can handle TransportCarbonException appropriately

### Maintainability
- **Single responsibility**: Each class has one reason to change
- **Clear contracts**: Port defines what services expect from external APIs
- **Easy to test**: Mock the port in service tests

## Tradeoffs

### Complexity
- More classes and interfaces than direct HTTP calls
- Initial setup overhead

### Mitigation
- Clear separation of concerns justifies the complexity
- Easy to understand and modify in the future
- Follows industry best practices (Hexagonal Architecture)

## Implementation Strategy

### Phase 1: Foundation (Current Sprint)
- Create Port interface (TransportCarbonPort)
- Create domain models (TransportRequest, TransportCarbonMetrics)
- Implement ImpactCo2TransportAdapter
- Set up DI configuration

### Phase 2: Integration (Next Sprint)
- Integrate into ProductTransportService
- Add unit tests for adapter
- Add integration tests with real API (optional)

### Phase 3: Enhancement (Future)
- Add caching layer for API responses
- Implement circuit breaker pattern for resilience
- Add metrics/monitoring for API calls
- Support additional external APIs using same pattern

## Consequences

### Positive
- **Decoupled design**: Domain logic independent of external APIs
- **Testable**: Easy to test with mocks
- **Maintainable**: Clear separation of concerns
- **Extensible**: Easy to add more external APIs
- **Resilient**: Centralized error handling

### Negative
- **More code**: Additional classes for mapping and abstraction
- **Learning curve**: Team needs to understand Ports & Adapters pattern

## Alternatives Considered

### 1. Direct HTTP Calls in Services
- **Pros**: Simpler, less boilerplate
- **Cons**: Couples domain logic to HTTP, hard to test, violates clean architecture

### 2. Spring Cloud Feign Client
- **Pros**: Declarative HTTP client
- **Cons**: Still exposes HTTP details to domain, less control over mapping

### 3. Event-Driven Integration
- **Pros**: Decoupled, asynchronous
- **Cons**: More complex, overkill for simple API calls

## References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Ports and Adapters Pattern](https://en.wikipedia.org/wiki/Hexagonal_architecture)
- [Impact CO2 API Documentation](https://impactco2.fr/doc/api)
- [Spring RestTemplate Best Practices](https://www.baeldung.com/rest-template)

---

**Decision Date**: 2025-03-13  
**Author**: Development Team  
**Status**: Proposed  
**Review Date**: 2025-03-20  
**Next Steps**: Implement Port and Adapter for Impact CO2 API
