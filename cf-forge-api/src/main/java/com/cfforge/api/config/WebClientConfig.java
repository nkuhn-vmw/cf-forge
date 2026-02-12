package com.cfforge.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${cf.api.endpoint:https://api.sys.example.com}")
    private String cfApiEndpoint;

    @Bean
    public WebClient cfApiWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
            .build();
        return WebClient.builder()
            .baseUrl(cfApiEndpoint + "/v3")
            .exchangeStrategies(strategies)
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
