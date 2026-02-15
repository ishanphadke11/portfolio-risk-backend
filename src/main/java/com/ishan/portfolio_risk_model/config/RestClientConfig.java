package com.ishan.portfolio_risk_model.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${flask.service.base-url}")
    private String flaskBaseUrl;

    @Bean
    public RestClient flaskRestClient() {
        return RestClient.builder()
                .baseUrl(flaskBaseUrl)
                .build();
    }

}
