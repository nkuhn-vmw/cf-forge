package com.cfforge.agent.service;

import com.cfforge.agent.model.EvaluationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EvaluationService {

    private final ChatClient chatClient;

    public EvaluationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public EvaluationResult evaluate(String generatedCode, String originalPrompt) {
        return chatClient.prompt()
            .system("""
                You are a code quality judge. Evaluate the generated code on these criteria:
                1. Correctness (0-10): Does it fulfill the user's requirements?
                2. CF Best Practices (0-10): Does it follow Cloud Foundry patterns (12-factor, service bindings, health checks)?
                3. Security (0-10): No hardcoded secrets, proper input validation, secure defaults?
                4. Production Readiness (0-10): Error handling, logging, configuration externalization?
                5. Code Quality (0-10): Clean code, proper structure, appropriate patterns?

                Provide an overall score (average), a brief summary, and specific improvement suggestions.
                """)
            .user("Original prompt: " + originalPrompt + "\n\nGenerated code:\n" + generatedCode)
            .call()
            .entity(EvaluationResult.class);
    }
}
