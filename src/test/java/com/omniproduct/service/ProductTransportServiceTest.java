package com.omniproduct.service;

import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTransportServiceTest {

    // Un port en mémoire : 0,1 kg de CO2 par tonne-kilomètre, quel que soit le mode.
    private final ProductTransportService service = new ProductTransportService(request ->
            new TransportCarbonMetrics(request.distanceKm() * request.weightKg() / 10_000, 0.0,
                    request.transportMode(), request.distanceKm(), request.weightKg()));

    @Test
    void asksThePortForTheShipmentAndReturnsItsMetrics() {
        TransportCarbonMetrics metrics = service.calculateShipmentCarbon("truck", 500.0, 200.0);

        assertThat(metrics.co2EquivalentKg()).isEqualTo(10.0);
        assertThat(metrics.transportMode()).isEqualTo("truck");
        assertThat(metrics.distance()).isEqualTo(500.0);
        assertThat(metrics.weight()).isEqualTo(200.0);
    }

    @Test
    void refusesAnInvalidShipmentBeforeCallingThePort() {
        ProductTransportService failing = new ProductTransportService(request -> {
            throw new AssertionError("the port must not be called");
        });

        assertThatThrownBy(() -> failing.calculateShipmentCarbon("truck", 0.0, 200.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Distance must be positive");
        assertThatThrownBy(() -> failing.calculateShipmentCarbon(" ", 500.0, 200.0))
                .hasMessage("Transport mode is required");
    }
}
