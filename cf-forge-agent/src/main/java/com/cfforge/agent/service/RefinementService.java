package com.cfforge.agent.service;

import com.cfforge.agent.model.GeneratedFile;
import com.cfforge.common.events.MetricEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.util.ResourceUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RefinementService {

    private final ChatClient chatClient;
    private final MetricEventPublisher metricPublisher;

    public RefinementService(ChatClient chatClient, MetricEventPublisher metricPublisher) {
        this.chatClient = chatClient;
        this.metricPublisher = metricPublisher;
    }

    public Flux<String> refine(UUID conversationId, String userFeedback, String currentFiles) {
        long start = System.currentTimeMillis();

        return chatClient.prompt()
            .system(ResourceUtils.getText("classpath:prompts/refinement.st"))
            .user("User feedback: " + userFeedback + "\n\nCurrent project files:\n" + currentFiles)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
            .stream()
            .content()
            .doOnComplete(() -> {
                long duration = System.currentTimeMillis() - start;
                metricPublisher.publishSuccess("agent.refine", null, null, (int) duration);
            })
            .doOnError(e -> metricPublisher.publishFailure("agent.refine", null, null, e.getMessage()));
    }

    public List<GeneratedFile> refineStructured(UUID conversationId, String userFeedback, String currentFiles) {
        return chatClient.prompt()
            .system(ResourceUtils.getText("classpath:prompts/refinement.st") +
                    "\n\nReturn ONLY the files that need changes as a JSON array. " +
                    "Use action CREATE for new files, MODIFY for changed files, DELETE for removed files.")
            .user("User feedback: " + userFeedback + "\n\nCurrent project files:\n" + currentFiles)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
            .call()
            .entity(new ParameterizedTypeReference<>() {});
    }
}
