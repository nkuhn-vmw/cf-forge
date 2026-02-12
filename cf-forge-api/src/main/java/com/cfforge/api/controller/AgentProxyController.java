package com.cfforge.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * Proxies AI agent requests from the UI to the internal agent service.
 * The generate endpoint accepts GET (EventSource-compatible) and forwards
 * as POST to the agent service's SSE streaming endpoint.
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentProxyController {

    private final WebClient agentClient;

    public AgentProxyController(@Qualifier("agentWebClient") WebClient agentClient) {
        this.agentClient = agentClient;
    }

    @GetMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(@RequestParam String projectId, @RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(300_000L);

        Map<String, String> body = Map.of(
            "conversationId", UUID.randomUUID().toString(),
            "projectId", projectId,
            "message", prompt
        );

        agentClient.post()
            .uri("/api/v1/agent/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .doOnNext(data -> {
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnError(emitter::completeWithError)
            .doOnComplete(emitter::complete)
            .subscribe();

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    @PostMapping("/structured")
    public Map<?, ?> generateStructured(@RequestBody Map<String, String> body) {
        return agentClient.post()
            .uri("/api/v1/agent/structured")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
