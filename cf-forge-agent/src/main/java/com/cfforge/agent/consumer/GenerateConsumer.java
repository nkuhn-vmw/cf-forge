package com.cfforge.agent.consumer;

import com.cfforge.agent.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
public class GenerateConsumer {

    private final AgentService agentService;

    public GenerateConsumer(AgentService agentService) {
        this.agentService = agentService;
    }

    @Bean
    public Consumer<Map<String, String>> agentGenerate() {
        return request -> {
            UUID conversationId = UUID.fromString(request.get("conversationId"));
            UUID projectId = UUID.fromString(request.get("projectId"));
            String message = request.get("message");
            log.info("Processing generate request for project: {}", projectId);
            agentService.generate(conversationId, projectId, message).blockLast();
        };
    }
}
