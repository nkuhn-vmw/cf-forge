package com.cfforge.agent.config;

import com.cfforge.agent.advisor.CfContextAdvisor;
import com.cfforge.agent.advisor.CodeSafetyAdvisor;
import com.cfforge.agent.advisor.OperatorInstructionsAdvisor;
import com.cfforge.agent.advisor.PromptInjectionAdvisor;
import com.cfforge.agent.advisor.RecursiveRefinementAdvisor;
import com.cfforge.agent.advisor.ToolArgumentAugmenter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.util.ResourceUtils;
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
            CfContextAdvisor cfContextAdvisor,
            CodeSafetyAdvisor codeSafetyAdvisor,
            OperatorInstructionsAdvisor operatorInstructionsAdvisor,
            PromptInjectionAdvisor promptInjectionAdvisor,
            ToolArgumentAugmenter toolArgumentAugmenter,
            RecursiveRefinementAdvisor recursiveRefinementAdvisor,
            QuestionAnswerAdvisor cfDocsRagAdvisor,
            ToolCallingManager toolCallingManager) {

        return builder
            .defaultSystem(ResourceUtils.getText("classpath:prompts/system.st"))
            .defaultAdvisors(
                promptInjectionAdvisor,
                operatorInstructionsAdvisor,
                cfContextAdvisor,
                toolArgumentAugmenter,
                MessageChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                        .chatMemoryRepository(chatMemoryRepository)
                        .maxMessages(50)
                        .build()
                ).build(),
                cfDocsRagAdvisor,
                ToolCallAdvisor.builder().toolCallingManager(toolCallingManager).build(),
                codeSafetyAdvisor,
                recursiveRefinementAdvisor
            )
            .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
            .dataSource(dataSource)
            .build();
    }
}
