package com.cfforge.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Proxies AI benchmark evaluation requests to the internal agent service.
 */
@RestController
@RequestMapping("/api/v1/bench")
public class BenchmarkProxyController {

    private final WebClient agentClient;

    public BenchmarkProxyController(@Qualifier("agentWebClient") WebClient agentClient) {
        this.agentClient = agentClient;
    }

    @PostMapping("/evaluate")
    public Map<?, ?> evaluate(@RequestBody Map<String, String> request) {
        return agentClient.post()
            .uri("/api/v1/bench/evaluate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
