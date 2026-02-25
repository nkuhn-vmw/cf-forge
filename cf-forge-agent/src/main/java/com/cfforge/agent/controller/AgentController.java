package com.cfforge.agent.controller;

import com.cfforge.agent.model.GeneratedAppPlan;
import com.cfforge.agent.service.AgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    public AgentController(AgentService agentService, ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generate(@RequestBody Map<String, String> body) {
        UUID conversationId = UUID.fromString(body.get("conversationId"));
        UUID projectId = parseUuidOrNull(body.get("projectId"));
        String message = body.get("message");
        // JSON-encode each token to preserve whitespace through SSE transport
        return agentService.generate(conversationId, projectId, message)
            .map(token -> {
                try {
                    return objectMapper.writeValueAsString(Map.of("token", token));
                } catch (JsonProcessingException e) {
                    return "{\"token\":" + token + "}";
                }
            });
    }

    @PostMapping("/structured")
    public GeneratedAppPlan generateStructured(@RequestBody Map<String, String> body) {
        UUID projectId = parseUuidOrNull(body.get("projectId"));
        String message = body.get("message");
        return agentService.generateStructured(projectId, message, GeneratedAppPlan.class);
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
