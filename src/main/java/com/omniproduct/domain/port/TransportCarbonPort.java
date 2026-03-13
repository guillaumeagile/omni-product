package com.omniproduct.domain.port;

import com.omniproduct.domain.model.TransportCarbonMetrics;
import com.omniproduct.domain.model.TransportRequest;

public interface TransportCarbonPort {
    
    TransportCarbonMetrics calculateEmissions(TransportRequest transportRequest);
}
