package com.omniproduct.service;

import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import com.omniproduct.domain.port.TransportCarbonPort;
import org.springframework.stereotype.Service;

@Service
public class ProductTransportService {
    
    private final TransportCarbonPort transportCarbonPort;
    
    public ProductTransportService(TransportCarbonPort transportCarbonPort) {
        this.transportCarbonPort = transportCarbonPort;
    }
    
    public TransportCarbonMetrics calculateShipmentCarbon(
            String transportMode,
            Double distanceKm,
            Double weightKg) {
        
        TransportRequest request = new TransportRequest(
            transportMode,
            distanceKm,
            weightKg
        );
        
        return transportCarbonPort.calculateEmissions(request);
    }
}
