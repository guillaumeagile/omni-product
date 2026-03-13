package com.omniproduct.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ImpactCo2ApiClient {
    
    private static final String BASE_URL = "https://impactco2.fr/api/v1";
    private static final String TRANSPORT_ENDPOINT = "/transport/emissions";
    
    private final RestTemplate restTemplate;
    
    public ImpactCo2ApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public ImpactCo2Response calculateEmissions(ImpactCo2Request request) {
        String url = BASE_URL + TRANSPORT_ENDPOINT;
        return restTemplate.postForObject(url, request, ImpactCo2Response.class);
    }
}
