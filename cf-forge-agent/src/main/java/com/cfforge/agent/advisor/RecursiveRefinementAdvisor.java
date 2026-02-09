package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RecursiveRefinementAdvisor implements CallAdvisor {

    private static final String RECURSION_DEPTH_KEY = "recursive_refinement_depth";

    @Value("${cf.forge.agent.max-refinement-depth:2}")
    private int maxDepth;

    private static final List<String> QUALITY_ISSUES = List.of(
        "// TODO", "// FIXME", "/* TODO",
        "throw new UnsupportedOperationException",
        "NotImplementedError", "pass  # TODO",
        "PLACEHOLDER", "...", "YOUR_", "CHANGE_ME", "REPLACE_THIS"
    );

    private static final List<String> INCOMPLETE_SIGNALS = List.of(
        "I'll complete this later", "implementation left as exercise",
        "remaining steps are similar", "and so on", "etc.", "fill in the rest"
    );

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int currentDepth = getRecursionDepth(request);

        ChatClientResponse response = chain.nextCall(request);

        if (currentDepth >= maxDepth) {
            log.debug("Max refinement depth {} reached, returning as-is", maxDepth);
            return response;
        }

        String responseText = extractResponseText(response);
        if (responseText == null || responseText.isBlank()) {
            return response;
        }

        List<String> issues = detectQualityIssues(responseText);
        if (issues.isEmpty()) {
            return response;
        }

        log.info("Detected {} quality issues at depth {}, triggering refinement", issues.size(), currentDepth);

        String refinementInstruction = buildRefinementPrompt(responseText, issues);

        var refinedRequest = request.mutate()
            .prompt(request.prompt().augmentUserMessage(refinementInstruction))
            .context(RECURSION_DEPTH_KEY, currentDepth + 1)
            .build();

        return chain.nextCall(refinedRequest);
    }

    private int getRecursionDepth(ChatClientRequest request) {
        Object depth = request.context().get(RECURSION_DEPTH_KEY);
        if (depth instanceof Integer i) return i;
        return 0;
    }

    private String extractResponseText(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) return null;
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) return null;
        Generation gen = chatResponse.getResults().getFirst();
        if (gen.getOutput() == null) return null;
        return gen.getOutput().getText();
    }

    private List<String> detectQualityIssues(String text) {
        List<String> found = new ArrayList<>();

        for (String pattern : QUALITY_ISSUES) {
            if (text.contains(pattern)) {
                found.add("Contains placeholder: " + pattern);
            }
        }

        String lower = text.toLowerCase();
        for (String signal : INCOMPLETE_SIGNALS) {
            if (lower.contains(signal.toLowerCase())) {
                found.add("Incomplete signal: " + signal);
            }
        }

        long openBlocks = text.chars().filter(c -> c == '{').count();
        long closeBlocks = text.chars().filter(c -> c == '}').count();
        if (openBlocks > 0 && Math.abs(openBlocks - closeBlocks) > 2) {
            found.add("Unbalanced braces (possible truncation)");
        }

        return found;
    }

    private String buildRefinementPrompt(String originalResponse, List<String> issues) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Your previous response had quality issues that need to be fixed. ");
        prompt.append("Please provide a complete, production-ready version.\n\n");
        prompt.append("Issues detected:\n");
        for (String issue : issues) {
            prompt.append("- ").append(issue).append("\n");
        }
        prompt.append("\nPrevious response to refine:\n");
        if (originalResponse.length() > 2000) {
            prompt.append(originalResponse, 0, 2000);
            prompt.append("\n... (truncated)\n");
        } else {
            prompt.append(originalResponse);
        }
        prompt.append("\n\nPlease provide the complete, corrected version with all TODOs, ");
        prompt.append("placeholders, and incomplete sections fully implemented.");
        return prompt.toString();
    }

    @Override
    public String getName() { return "RecursiveRefinementAdvisor"; }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 200; }
}
