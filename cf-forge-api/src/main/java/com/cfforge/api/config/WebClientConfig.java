package com.cfforge.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${cf.api.endpoint:https://api.sys.example.com}")
    private String cfApiEndpoint;

    @Bean
    public WebClient cfApiWebClient() {
        return WebClient.builder()
            .baseUrl(cfApiEndpoint + "/v3")
            .build();
    }

    @Bean
    public WebClient agentWebClient() {
        return WebClient.builder()
            .baseUrl("http://cf-forge-agent.apps.internal:8080")
            .build();
    }

    @Bean
    public WebClient workspaceWebClient() {
        return WebClient.builder()
            .baseUrl("http://cf-forge-workspace.apps.internal:8080")
            .build();
    }
}
