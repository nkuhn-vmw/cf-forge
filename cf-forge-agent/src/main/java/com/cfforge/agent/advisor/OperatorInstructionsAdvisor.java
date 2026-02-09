package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

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
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        String instructions = loadInstructions();
        if (instructions != null) {
            var augmented = AdvisedRequest.from(request)
                .withSystemText(request.systemText() + "\n\nOPERATOR INSTRUCTIONS:\n" + instructions)
                .build();
            return chain.nextCall(augmented);
        }
        return chain.nextCall(request);
    }

    @Override
    public reactor.core.publisher.Flux<AdvisedResponse> adviseStream(AdvisedRequest request, StreamAdvisorChain chain) {
        String instructions = loadInstructions();
        if (instructions != null) {
            var augmented = AdvisedRequest.from(request)
                .withSystemText(request.systemText() + "\n\nOPERATOR INSTRUCTIONS:\n" + instructions)
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
            Path classpathFile = Path.of("classpath:" + INSTRUCTIONS_FILE);
            if (Files.exists(classpathFile)) {
                cachedInstructions = Files.readString(classpathFile);
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
