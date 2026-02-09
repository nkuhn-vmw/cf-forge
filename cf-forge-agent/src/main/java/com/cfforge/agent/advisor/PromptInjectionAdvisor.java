package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PromptInjectionAdvisor implements CallAdvisor {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("(?i)ignore (all |previous |prior |above )?instructions"),
        Pattern.compile("(?i)disregard (all |previous |prior |above )?instructions"),
        Pattern.compile("(?i)forget (all |previous |your )?instructions"),
        Pattern.compile("(?i)override (your |the |system )?prompt"),
        Pattern.compile("(?i)you are now (a |an )?"),
        Pattern.compile("(?i)new (role|persona|identity|instructions)\\s*:"),
        Pattern.compile("(?i)(show|reveal|display|print|output|tell me) (your |the )?(system|initial|original) (prompt|instructions|message)"),
        Pattern.compile("(?i)what (are|were) your (original |initial |system )?instructions"),
        Pattern.compile("(?i)(```|---|\\[SYSTEM\\]|<\\|im_start\\|>|<\\|system\\|>)"),
        Pattern.compile("(?i)\\[\\s*(INST|SYS|SYSTEM)\\s*\\]"),
        Pattern.compile("(?i)pretend (you are|to be|you're) (not |no longer )?"),
        Pattern.compile("(?i)act as (if|though) you (have |had )?(no|don't have|lack) (restrictions|limits|rules|guidelines)"),
        Pattern.compile("(?i)base64 (decode|encoded|encoding)"),
        Pattern.compile("(?i)rot13"),
        Pattern.compile("(?i)decode the following")
    );

    private static final double INJECTION_THRESHOLD = 2;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        var userMessage = request.prompt().getUserMessage();
        if (userMessage != null) {
            String userText = userMessage.getText();
            if (userText != null) {
                int matchCount = 0;
                List<String> matchedPatterns = new ArrayList<>();

                for (Pattern p : INJECTION_PATTERNS) {
                    if (p.matcher(userText).find()) {
                        matchCount++;
                        matchedPatterns.add(p.pattern());
                    }
                }

                if (matchCount >= INJECTION_THRESHOLD) {
                    log.warn("PROMPT INJECTION DETECTED ({} pattern matches): {}", matchCount, matchedPatterns);
                    var safeMessage = new AssistantMessage(
                        "I'm sorry, but I can't process that request. It appears to contain " +
                        "instructions that conflict with my guidelines. Please rephrase your " +
                        "request to focus on building or deploying your Cloud Foundry application.");
                    var generation = new Generation(safeMessage);
                    var chatResponse = new ChatResponse(List.of(generation));
                    return new ChatClientResponse(chatResponse, request.context());
                }

                if (matchCount > 0) {
                    log.info("Prompt injection warning ({} pattern match, below threshold): {}", matchCount, matchedPatterns);
                }
            }
        }

        return chain.nextCall(request);
    }

    @Override
    public String getName() { return "PromptInjectionAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 50; }
}
