package com.cfforge.agent.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) Server Registry (ECO-002).
 *
 * Manages connections to external MCP servers (Jira, Confluence, ServiceNow, etc.)
 * that provide additional context and tools to the AI agent. Each MCP server
 * exposes resources and tools via the MCP protocol, allowing the agent to
 * read from and write to external systems.
 */
@Component
@Slf4j
public class McpServerRegistry {

    private final Map<String, McpServerConfig> servers = new ConcurrentHashMap<>();

    public void register(McpServerConfig config) {
        servers.put(config.getName(), config);
        log.info("Registered MCP server: {} at {}", config.getName(), config.getEndpoint());
    }

    public void unregister(String name) {
        servers.remove(name);
        log.info("Unregistered MCP server: {}", name);
    }

    public Optional<McpServerConfig> get(String name) {
        return Optional.ofNullable(servers.get(name));
    }

    public Collection<McpServerConfig> listAll() {
        return servers.values();
    }

    public List<McpToolDescriptor> getAvailableTools() {
        List<McpToolDescriptor> allTools = new ArrayList<>();
        for (McpServerConfig server : servers.values()) {
            if (server.isEnabled()) {
                server.getTools().forEach(tool ->
                    allTools.add(new McpToolDescriptor(
                        server.getName() + "." + tool.getName(),
                        tool.getDescription(),
                        server.getName(),
                        tool.getInputSchema()
                    ))
                );
            }
        }
        return allTools;
    }

    @Data
    public static class McpServerConfig {
        private String name;
        private String endpoint;
        private String type; // "jira", "confluence", "servicenow", "custom"
        private String authType; // "bearer", "basic", "api-key"
        private String authToken;
        private boolean enabled = true;
        private List<McpTool> tools = new ArrayList<>();
        private Map<String, String> metadata = new HashMap<>();
    }

    @Data
    public static class McpTool {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
    }

    public record McpToolDescriptor(String qualifiedName, String description,
                                     String serverName, Map<String, Object> inputSchema) {}
}
