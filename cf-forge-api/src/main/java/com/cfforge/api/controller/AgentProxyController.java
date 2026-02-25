package com.cfforge.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;
import java.util.UUID;

/**
 * Proxies AI agent requests from the UI to the internal agent service.
 * Streams raw SSE bytes through without re-parsing to preserve token whitespace.
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentProxyController {

    private final WebClient agentClient;

    public AgentProxyController(@Qualifier("agentWebClient") WebClient agentClient) {
        this.agentClient = agentClient;
    }

    @GetMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> generate(
            @RequestParam String projectId, @RequestParam String prompt) {
        Map<String, String> body = Map.of(
            "conversationId", UUID.randomUUID().toString(),
            "projectId", projectId,
            "message", prompt
        );
        return streamFromAgent(body);
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> generatePost(@RequestBody Map<String, String> body) {
        return streamFromAgent(body);
    }

    private ResponseEntity<StreamingResponseBody> streamFromAgent(Map<String, String> body) {
        StreamingResponseBody responseBody = outputStream -> {
            agentClient.post()
                .uri("/api/v1/agent/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnNext(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        outputStream.write(bytes);
                        outputStream.flush();
                    } catch (Exception e) {
                        // Client disconnected
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .doOnComplete(() -> {
                    try { outputStream.close(); } catch (Exception ignored) {}
                })
                .doOnError(e -> {
                    try { outputStream.close(); } catch (Exception ignored) {}
                })
                .blockLast();
        };

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(responseBody);
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
