package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CodeSafetyAdvisor implements CallAdvisor {

    @Override
    public AdvisedResponse adviseCall(AdvisedRequest request, CallAdvisorChain chain) {
        AdvisedResponse response = chain.nextCall(request);
        String content = response.result().getOutput().getText();
        if (content != null && containsHardcodedSecrets(content)) {
            log.warn("Generated code may contain hardcoded secrets - flagging for review");
        }
        return response;
    }

    private boolean containsHardcodedSecrets(String content) {
        String lower = content.toLowerCase();
        return lower.contains("password=") && !lower.contains("${") && !lower.contains("vcap")
            || lower.contains("api_key=") && !lower.contains("${");
    }

    @Override
    public String getName() { return "CodeSafetyAdvisor"; }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 100; }
}
