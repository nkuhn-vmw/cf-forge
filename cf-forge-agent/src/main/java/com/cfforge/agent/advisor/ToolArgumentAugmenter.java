package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ToolArgumentAugmenter implements CallAdvisor {

    private volatile Map<String, String> buildpackAliases;
    private volatile Map<String, String> serviceDefaults;
    private volatile long lastRefresh;
    private static final long CACHE_TTL = 300_000; // 5 minutes

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        refreshContext();

        String augmentation = buildToolContext();
        var augmented = request.mutate()
            .prompt(request.prompt().augmentSystemMessage("\n\nTOOL ARGUMENT CONTEXT:\n" + augmentation))
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
        serviceDefaults.put("postgresql", "small");
        serviceDefaults.put("redis", "shared-vm");
        serviceDefaults.put("rabbitmq", "standard");
        serviceDefaults.put("p.mysql", "db-small");

        lastRefresh = now;
    }

    @Override
    public String getName() { return "ToolArgumentAugmenter"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 200; }
}
