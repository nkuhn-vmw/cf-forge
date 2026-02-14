package com.cfforge.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private ExchangeStrategies exchangeStrategies() {
        return ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
            .build();
    }

    @Bean
    public WebClient cfApiClient(
            @Value("${cfforge.cf-api.url:https://api.sys.local}") String cfApiUrl,
            @Value("${cfforge.cf-api.token:}") String cfApiToken) {
        var builder = WebClient.builder()
            .baseUrl(cfApiUrl + "/v3")
            .exchangeStrategies(exchangeStrategies());
        if (cfApiToken != null && !cfApiToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + cfApiToken);
        }
        return builder.build();
    }

    @Bean
    public WebClient workspaceClient(
            @Value("${cfforge.workspace.url:http://cf-forge-workspace.apps.internal:8080}") String workspaceUrl) {
        return WebClient.builder()
            .baseUrl(workspaceUrl)
            .exchangeStrategies(exchangeStrategies())
            .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .exchangeStrategies(exchangeStrategies());
    }
}
