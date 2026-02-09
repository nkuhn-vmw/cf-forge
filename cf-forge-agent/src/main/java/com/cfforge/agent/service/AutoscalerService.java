package com.cfforge.agent.service;

import com.cfforge.agent.model.AutoscalerPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AutoscalerService {

    private final ChatClient chatClient;

    public AutoscalerService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AutoscalerPolicy generatePolicy(String appType, String expectedLoad) {
        return chatClient.prompt()
            .system("Generate a CF Autoscaler policy for the described application. " +
                    "Include sensible scaling rules based on CPU, memory, HTTP throughput, or HTTP latency. " +
                    "Set conservative min/max instance counts appropriate for the workload.")
            .user("App type: " + appType + ", Expected load: " + expectedLoad)
            .call()
            .entity(AutoscalerPolicy.class);
    }

    public AutoscalerPolicy refinePolicy(AutoscalerPolicy current, String feedback) {
        return chatClient.prompt()
            .system("Refine the given CF Autoscaler policy based on the user feedback. " +
                    "Keep the same JSON structure.")
            .user("Current policy:\n" + current + "\n\nFeedback: " + feedback)
            .call()
            .entity(AutoscalerPolicy.class);
    }
}
