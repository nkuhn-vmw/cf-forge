package com.cfforge.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Proxies multi-agent workflow requests to the internal agent service.
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentWorkflowProxyController {

    private final WebClient agentClient;

    public AgentWorkflowProxyController(@Qualifier("agentWebClient") WebClient agentClient) {
        this.agentClient = agentClient;
    }

    @GetMapping
    public List<?> listAgents() {
        return agentClient.get()
            .uri("/api/v1/agents")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .block();
    }

    @PostMapping("/workflow")
    public Map<?, ?> executeWorkflow(@RequestBody Map<String, Object> request) {
        return agentClient.post()
            .uri("/api/v1/agents/workflow")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
