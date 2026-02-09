package com.cfforge.agent.service;

import com.cfforge.agent.model.CfManifest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ResourceUtils;
import org.springframework.stereotype.Service;

@Service
public class ManifestGeneratorService {

    private final ChatClient chatClient;

    public ManifestGeneratorService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public CfManifest generateManifest(String appDescription, String language, String framework) {
        return chatClient.prompt()
            .system(ResourceUtils.getText("classpath:prompts/manifest.st"))
            .user("Generate a CF manifest for: " + appDescription +
                  "\nLanguage: " + language + "\nFramework: " + framework)
            .call()
            .entity(CfManifest.class);
    }
}
