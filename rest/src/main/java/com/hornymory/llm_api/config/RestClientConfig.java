package com.hornymory.llm_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
        @Bean
        RestClient coreRestClient(RestClient.Builder b, @Value("${ai.core.base-url}") String baseUrl) {
            return b.baseUrl(baseUrl).build();
        }
}
