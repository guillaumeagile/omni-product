package com.omniproduct.infrastructure.adapter;

import com.omniproduct.domain.exception.TransportCarbonException;
import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;
import com.omniproduct.domain.port.TransportCarbonPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class ImpactCo2TransportAdapter implements TransportCarbonPort {
    
    private final ImpactCo2ApiClient apiClient;
    
    public ImpactCo2TransportAdapter(ImpactCo2ApiClient apiClient) {
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
    
    private ImpactCo2Request mapToApiRequest(TransportRequest transportRequest) {
        return new ImpactCo2Request(
            transportRequest.transportMode(),
            transportRequest.distanceKm(),
            transportRequest.weightKg()
        );
    }
    
    private TransportCarbonMetrics mapToDomainModel(ImpactCo2Response response) {
        return new TransportCarbonMetrics(
            response.co2Equivalent(),
            response.co2Direct(),
            response.transportMode(),
            response.distance(),
            response.weight()
        );
    }
}
