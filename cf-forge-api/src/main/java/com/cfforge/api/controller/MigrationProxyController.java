package com.cfforge.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Proxies migration analysis requests to the internal agent service.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class MigrationProxyController {

    private final WebClient agentClient;

    public MigrationProxyController(@Qualifier("agentWebClient") WebClient agentClient) {
        this.agentClient = agentClient;
    }

    @PostMapping("/analyze")
    public Map<?, ?> analyze(@RequestBody Map<String, String> request) {
        return agentClient.post()
            .uri("/api/v1/migration/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
