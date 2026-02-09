package com.cfforge.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.support.ResourceUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agentChatClient(
            ChatClient.Builder builder,
            ChatMemoryRepository chatMemoryRepository,
            List<Object> agentTools) {

        return builder
            .defaultSystem(ResourceUtils.getText("classpath:prompts/system.st"))
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                        .chatMemoryRepository(chatMemoryRepository)
                        .maxMessages(50)
                        .build()
                ).build(),
                new ToolCallAdvisor()
            )
            .defaultTools(agentTools.toArray())
            .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
            .dataSource(dataSource)
            .build();
    }
}
