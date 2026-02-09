package com.cfforge.agent.service;

import com.cfforge.agent.model.ServiceRecommendation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class MarketplaceService {

    private final ChatClient chatClient;
    private final WebClient cfApiClient;

    public MarketplaceService(ChatClient chatClient, WebClient cfApiClient) {
        this.chatClient = chatClient;
        this.cfApiClient = cfApiClient;
    }

    public List<ServiceRecommendation> recommendServices(String appDescription) {
        String marketplaceInfo = fetchMarketplaceSummary();

        return chatClient.prompt()
            .system("Given the CF marketplace services, recommend which services this application needs. " +
                    "For each recommendation, provide the service name, plan, reason for recommendation, " +
                    "and a suggested binding name. Only recommend services that exist in the marketplace.")
            .user(appDescription + "\n\nAvailable marketplace services:\n" + marketplaceInfo)
            .call()
            .entity(new ParameterizedTypeReference<>() {});
    }

    private String fetchMarketplaceSummary() {
        try {
            return cfApiClient.get()
                .uri("/v3/service_offerings")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            return "PostgreSQL (small, medium, large), Redis (cache-small, cache-medium), " +
                   "RabbitMQ (single, ha), GenAI (standard), p-identity (auth), " +
                   "S3-compatible storage (standard)";
        }
    }
}
