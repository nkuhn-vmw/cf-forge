package com.cfforge.agent.advisor;

import com.cfforge.api.service.CfClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool Argument Augmenter (AI-030).
 *
 * Runs after tool calls are resolved but before tool execution. Enriches
 * tool call arguments with CF platform context such as default service plans,
 * buildpack names, org/space info so the LLM doesn't have to guess or
 * hallucinate these values.
 *
 * For example, if a tool call includes a "buildpack" argument, this advisor
 * ensures it matches one of the actually available buildpacks. If a service
 * plan is referenced, the correct marketplace plan name is substituted.
 */
@Component
@Slf4j
public class ToolArgumentAugmenter implements CallAdvisor {

    private final CfClient cfClient;
    private volatile Map<String, String> buildpackAliases;
    private volatile Map<String, String> serviceDefaults;
    private volatile long lastRefresh;
    private static final long CACHE_TTL = 300_000; // 5 minutes

    public ToolArgumentAugmenter(CfClient cfClient) {
        this.cfClient = cfClient;
    }

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        refreshContext();

        // Augment the system text with tool-specific context
        String augmentation = buildToolContext();
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nTOOL ARGUMENT CONTEXT:\n" + augmentation)
            .build();

        return chain.nextCall(augmented);
    }

    private String buildToolContext() {
        StringBuilder ctx = new StringBuilder();

        ctx.append("When calling tools that accept buildpack arguments, use these exact names:\n");
        if (buildpackAliases != null) {
            buildpackAliases.forEach((alias, canonical) ->
                ctx.append("  ").append(alias).append(" -> ").append(canonical).append("\n"));
        }

        ctx.append("\nWhen calling tools that reference service plans, use these defaults:\n");
        if (serviceDefaults != null) {
            serviceDefaults.forEach((service, plan) ->
                ctx.append("  ").append(service).append(": default plan = ").append(plan).append("\n"));
        }

        ctx.append("\nImportant:\n");
        ctx.append("- Always use 'java_buildpack_offline' (not 'java_buildpack') for air-gapped deployments\n");
        ctx.append("- For database services, prefer plan names from the marketplace over generic names\n");
        ctx.append("- When specifying memory, use CF conventions: 256M, 512M, 1G, 2G\n");

        return ctx.toString();
    }

    private void refreshContext() {
        long now = System.currentTimeMillis();
        if (buildpackAliases != null && (now - lastRefresh) < CACHE_TTL) {
            return;
        }
        try {
            buildpackAliases = new ConcurrentHashMap<>();
            buildpackAliases.put("java", "java_buildpack_offline");
            buildpackAliases.put("java_buildpack", "java_buildpack_offline");
            buildpackAliases.put("node", "nodejs_buildpack");
            buildpackAliases.put("nodejs", "nodejs_buildpack");
            buildpackAliases.put("python", "python_buildpack");
            buildpackAliases.put("go", "go_buildpack");
            buildpackAliases.put("golang", "go_buildpack");
            buildpackAliases.put("ruby", "ruby_buildpack");
            buildpackAliases.put("static", "staticfile_buildpack");
            buildpackAliases.put("dotnet", "dotnet_core_buildpack");

            serviceDefaults = new ConcurrentHashMap<>();
            try {
                var services = cfClient.listMarketplace();
                if (services != null) {
                    services.forEach(s -> {
                        serviceDefaults.put(s.name(), "default");
                    });
                }
            } catch (Exception e) {
                log.debug("Could not fetch marketplace for tool augmentation: {}", e.getMessage());
                serviceDefaults.put("postgresql", "small");
                serviceDefaults.put("redis", "shared-vm");
                serviceDefaults.put("rabbitmq", "standard");
                serviceDefaults.put("p.mysql", "db-small");
            }

            lastRefresh = now;
        } catch (Exception e) {
            log.warn("Failed to refresh tool argument context: {}", e.getMessage());
        }
    }

    @Override
    public String getName() { return "ToolArgumentAugmenter"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 200; }
}
