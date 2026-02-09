package com.cfforge.agent.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class CfContextAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        String cfContext = "CF Foundation: Available | Services: PostgreSQL, Redis, RabbitMQ, GenAI | Buildpacks: java, python, nodejs, go, staticfile";
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nCF FOUNDATION STATUS:\n" + cfContext)
            .build();
        return chain.nextCall(augmented);
    }

    @Override
    public Flux<AdvisedResponse> adviseStream(AdvisedRequest request, StreamAdvisorChain chain) {
        String cfContext = "CF Foundation: Available | Services: PostgreSQL, Redis, RabbitMQ, GenAI";
        var augmented = AdvisedRequest.from(request)
            .withSystemText(request.systemText() + "\n\nCF FOUNDATION STATUS:\n" + cfContext)
            .build();
        return chain.nextStream(augmented);
    }

    @Override
    public String getName() { return "CfContextAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 100; }
}
