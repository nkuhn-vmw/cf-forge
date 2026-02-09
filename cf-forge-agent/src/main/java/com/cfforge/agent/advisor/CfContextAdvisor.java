package com.cfforge.agent.advisor;

import com.cfforge.api.service.CfClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class CfContextAdvisor implements CallAdvisor, StreamAdvisor {

    private final CfClient cfClient;
    private volatile String cachedContext;
    private volatile long lastRefresh;
    private static final long CACHE_TTL = 300_000; // 5 minutes

    public CfContextAdvisor(CfClient cfClient) {
        this.cfClient = cfClient;
    }

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        String cfContext = getCfFoundationContext();
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nCF FOUNDATION STATUS:\n" + cfContext)
            .build();
        return chain.nextCall(augmented);
    }

    @Override
    public Flux<AdvisedResponse> adviseStream(AdvisedRequest request, StreamAdvisorChain chain) {
        String cfContext = getCfFoundationContext();
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nCF FOUNDATION STATUS:\n" + cfContext)
            .build();
        return chain.nextStream(augmented);
    }

    private String getCfFoundationContext() {
        long now = System.currentTimeMillis();
        if (cachedContext != null && (now - lastRefresh) < CACHE_TTL) {
            return cachedContext;
        }
        try {
            StringBuilder ctx = new StringBuilder();
            ctx.append("Foundation: Available\n");

            var services = cfClient.listMarketplace();
            if (services != null) {
                ctx.append("Marketplace Services: ");
                ctx.append(String.join(", ", services.stream().map(s -> s.name()).toList()));
                ctx.append("\n");
            }

            var buildpacks = cfClient.listBuildpacks();
            if (buildpacks != null) {
                ctx.append("Buildpacks: ");
                ctx.append(String.join(", ", buildpacks.stream().map(b -> b.name()).toList()));
                ctx.append("\n");
            }

            try {
                var quota = cfClient.getOrgQuota();
                if (quota != null) {
                    ctx.append("Org Quota: ").append(quota.name())
                       .append(" (memory: ").append(quota.memoryLimit()).append("MB")
                       .append(", instances: ").append(quota.instanceLimit()).append(")\n");
                }
            } catch (Exception e) {
                log.debug("Could not fetch org quota: {}", e.getMessage());
            }

            cachedContext = ctx.toString();
            lastRefresh = now;
        } catch (Exception e) {
            log.warn("Failed to refresh CF context, using fallback: {}", e.getMessage());
            if (cachedContext == null) {
                cachedContext = "Foundation: Available | Services: PostgreSQL, Redis, RabbitMQ, GenAI | " +
                               "Buildpacks: java, python, nodejs, go, staticfile";
            }
        }
        return cachedContext;
    }

    @Override
    public String getName() { return "CfContextAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 100; }
}
