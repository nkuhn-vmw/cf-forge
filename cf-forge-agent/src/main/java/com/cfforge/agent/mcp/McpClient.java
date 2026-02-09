package com.cfforge.agent.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP Protocol Client.
 *
 * Communicates with external MCP servers to execute tool calls and
 * read resources. Supports JSON-RPC 2.0 over HTTP as per the MCP spec.
 */
@Component
@Slf4j
public class McpClient {

    private final WebClient.Builder webClientBuilder;

    public McpClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Map<String, Object>> callTool(McpServerRegistry.McpServerConfig server,
                                                String toolName,
                                                Map<String, Object> arguments) {
        WebClient client = buildClient(server);

        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", System.currentTimeMillis(),
            "method", "tools/call",
            "params", Map.of(
                "name", toolName,
                "arguments", arguments
            )
        );

        return client.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(McpResponse.class)
            .map(McpResponse::result)
            .doOnError(e -> log.error("MCP tool call failed: server={} tool={} error={}",
                server.getName(), toolName, e.getMessage()));
    }

    public Mono<Map<String, Object>> readResource(McpServerRegistry.McpServerConfig server,
                                                     String uri) {
        WebClient client = buildClient(server);

        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", System.currentTimeMillis(),
            "method", "resources/read",
            "params", Map.of("uri", uri)
        );

        return client.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(McpResponse.class)
            .map(McpResponse::result);
    }

    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listTools(McpServerRegistry.McpServerConfig server) {
        WebClient client = buildClient(server);

        Map<String, Object> request = Map.of(
            "jsonrpc", "2.0",
            "id", System.currentTimeMillis(),
            "method", "tools/list"
        );

        return client.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(McpResponse.class)
            .map(McpResponse::result);
    }

    private WebClient buildClient(McpServerRegistry.McpServerConfig server) {
        WebClient.Builder builder = webClientBuilder.baseUrl(server.getEndpoint());

        switch (server.getAuthType()) {
            case "bearer" -> builder.defaultHeader("Authorization", "Bearer " + server.getAuthToken());
            case "api-key" -> builder.defaultHeader("X-API-Key", server.getAuthToken());
            case "basic" -> builder.defaultHeader("Authorization", "Basic " + server.getAuthToken());
        }

        return builder.build();
    }

    public record McpResponse(String jsonrpc, Object id, Map<String, Object> result, Object error) {}
}
