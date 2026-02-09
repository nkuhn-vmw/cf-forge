package com.cfforge.agent.mcp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST API for managing MCP server connections.
 */
@RestController
@RequestMapping("/api/v1/mcp")
public class McpController {

    private final McpServerRegistry registry;
    private final McpClient mcpClient;

    public McpController(McpServerRegistry registry, McpClient mcpClient) {
        this.registry = registry;
        this.mcpClient = mcpClient;
    }

    @GetMapping("/servers")
    public Collection<McpServerRegistry.McpServerConfig> listServers() {
        return registry.listAll();
    }

    @PostMapping("/servers")
    public ResponseEntity<String> registerServer(@RequestBody McpServerRegistry.McpServerConfig config) {
        registry.register(config);
        // Discover tools from the server
        mcpClient.listTools(config).subscribe(result -> {
            if (result.containsKey("tools") && result.get("tools") instanceof List<?> tools) {
                for (Object t : tools) {
                    if (t instanceof Map<?, ?> toolMap) {
                        var tool = new McpServerRegistry.McpTool();
                        tool.setName((String) toolMap.get("name"));
                        tool.setDescription((String) toolMap.get("description"));
                        config.getTools().add(tool);
                    }
                }
            }
        });
        return ResponseEntity.ok("Registered: " + config.getName());
    }

    @DeleteMapping("/servers/{name}")
    public ResponseEntity<Void> unregisterServer(@PathVariable String name) {
        registry.unregister(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tools")
    public List<McpServerRegistry.McpToolDescriptor> listAvailableTools() {
        return registry.getAvailableTools();
    }

    @PostMapping("/tools/{serverName}/{toolName}")
    public ResponseEntity<Map<String, Object>> callTool(
            @PathVariable String serverName,
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {
        var server = registry.get(serverName)
            .orElseThrow(() -> new IllegalArgumentException("MCP server not found: " + serverName));
        Map<String, Object> result = mcpClient.callTool(server, toolName, arguments).block();
        return ResponseEntity.ok(result);
    }
}
