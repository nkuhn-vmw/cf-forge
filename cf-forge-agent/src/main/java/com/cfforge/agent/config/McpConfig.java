package com.cfforge.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class McpConfig {

    @Bean
    @Profile("mcp")
    public WebClient cfDocsMcpClient(@Value("${cfforge.mcp.cf-docs.url:https://cf-docs-mcp.apps.internal:8080}") String url) {
        return WebClient.builder()
            .baseUrl(url)
            .build();
    }

    @Bean
    @Profile("mcp")
    public WebClient jiraMcpClient(@Value("${cfforge.mcp.jira.url:https://jira-mcp.apps.internal:8080}") String url) {
        return WebClient.builder()
            .baseUrl(url)
            .build();
    }
}
