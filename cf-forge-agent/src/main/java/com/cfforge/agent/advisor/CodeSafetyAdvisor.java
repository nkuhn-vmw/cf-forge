package com.cfforge.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CodeSafetyAdvisor implements CallAdvisor {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
        Pattern.compile("(?i)(password|passwd|secret|api[_-]?key|token|credentials)\\s*[=:]\\s*[\"'][^${}\"']{4,}"),
        Pattern.compile("(?i)AKIA[0-9A-Z]{16}"),
        Pattern.compile("(?i)-----BEGIN (RSA |EC )?PRIVATE KEY-----"),
        Pattern.compile("(?i)ghp_[a-zA-Z0-9]{36}"),
        Pattern.compile("(?i)sk-[a-zA-Z0-9]{20,}")
    );

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec\\("),
        Pattern.compile("(?i)eval\\s*\\("),
        Pattern.compile("(?i)DROP\\s+TABLE|DELETE\\s+FROM.*WHERE\\s+1\\s*=\\s*1"),
        Pattern.compile("ProcessBuilder\\(.*\\$\\{"),
        Pattern.compile("(?i)<script[^>]*>")
    );

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        var chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.getResults() != null && !chatResponse.getResults().isEmpty()) {
            String content = chatResponse.getResults().getFirst().getOutput().getText();
            if (content != null) {
                validateContent(content);
            }
        }
        return response;
    }

    private void validateContent(String content) {
        for (Pattern p : SECRET_PATTERNS) {
            if (p.matcher(content).find()) {
                if (!content.contains("${") && !content.contains("VCAP_SERVICES")) {
                    log.warn("CODE SAFETY: Generated code may contain hardcoded secret (pattern: {})", p.pattern());
                }
            }
        }

        for (Pattern p : DANGEROUS_PATTERNS) {
            if (p.matcher(content).find()) {
                log.warn("CODE SAFETY: Generated code contains potentially dangerous pattern: {}", p.pattern());
            }
        }
    }

    @Override
    public String getName() { return "CodeSafetyAdvisor"; }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 100; }
}
