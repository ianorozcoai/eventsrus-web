package com.web.eventsrus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * The one real HTTP client to eventsrus-backend in this whole app - used
 * only for the subscription/billing/paywall feature (see AuthWebController,
 * VendorSubscriptionWebController). Every other page in eventsrus-web still
 * reads from StubDataService's local JSON, unchanged.
 */
@Configuration
public class BackendClientConfig {

    @Bean
    public RestClient backendRestClient(@Value("${eventsrus.backend.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
