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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class OperatorInstructionsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String INSTRUCTIONS_FILE = "forge-instructions.md";
    private volatile String cachedInstructions;
    private volatile long lastLoaded;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String instructions = loadInstructions();
        if (instructions != null) {
            var augmented = request.mutate()
                .prompt(request.prompt().augmentSystemMessage("\n\nOPERATOR INSTRUCTIONS:\n" + instructions))
                .build();
            return chain.nextCall(augmented);
        }
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String instructions = loadInstructions();
        if (instructions != null) {
            var augmented = request.mutate()
                .prompt(request.prompt().augmentSystemMessage("\n\nOPERATOR INSTRUCTIONS:\n" + instructions))
                .build();
            return chain.nextStream(augmented);
        }
        return chain.nextStream(request);
    }

    private String loadInstructions() {
        long now = System.currentTimeMillis();
        if (cachedInstructions != null && (now - lastLoaded) < 60_000) {
            return cachedInstructions;
        }
        try {
            Path path = Path.of(INSTRUCTIONS_FILE);
            if (Files.exists(path)) {
                cachedInstructions = Files.readString(path);
                lastLoaded = now;
                return cachedInstructions;
            }
        } catch (IOException e) {
            log.debug("No operator instructions found: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getName() { return "OperatorInstructionsAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 50; }
}
