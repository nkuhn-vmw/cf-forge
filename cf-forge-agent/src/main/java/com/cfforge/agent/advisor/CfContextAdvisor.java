package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class CfContextAdvisor implements CallAdvisor, StreamAdvisor {

    private volatile String cachedContext;
    private volatile long lastRefresh;
    private static final long CACHE_TTL = 300_000; // 5 minutes

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String cfContext = getCfFoundationContext();
        var augmented = request.mutate()
            .prompt(request.prompt().augmentSystemMessage("\n\nCF FOUNDATION STATUS:\n" + cfContext))
            .build();
        return chain.nextCall(augmented);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String cfContext = getCfFoundationContext();
        var augmented = request.mutate()
            .prompt(request.prompt().augmentSystemMessage("\n\nCF FOUNDATION STATUS:\n" + cfContext))
            .build();
        return chain.nextStream(augmented);
    }

    private String getCfFoundationContext() {
        long now = System.currentTimeMillis();
        if (cachedContext != null && (now - lastRefresh) < CACHE_TTL) {
            return cachedContext;
        }
        cachedContext = "Foundation: Available | Services: PostgreSQL, Redis, RabbitMQ, GenAI | " +
                       "Buildpacks: java, python, nodejs, go, staticfile";
        lastRefresh = now;
        return cachedContext;
    }

    @Override
    public String getName() { return "CfContextAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 100; }
}
