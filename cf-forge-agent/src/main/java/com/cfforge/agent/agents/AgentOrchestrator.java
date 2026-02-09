package com.cfforge.agent.agents;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;

/**
 * Spring AI Agents Framework Integration (ECO-008).
 *
 * Orchestrates multiple specialized AI agents that collaborate to complete
 * complex development tasks. Each agent has a specific role (architect,
 * developer, reviewer, deployer) and they communicate through a shared
 * context to deliver end-to-end solutions.
 *
 * Implements the agent pattern from Spring AI Agents 1.0 with:
 * - Agent registration and discovery
 * - Task decomposition and assignment
 * - Inter-agent communication via shared context
 * - Result aggregation and conflict resolution
 */
@Service
@Slf4j
public class AgentOrchestrator {

    private final ChatClient chatClient;
    private final Map<String, AgentDefinition> registeredAgents = new LinkedHashMap<>();

    public AgentOrchestrator(ChatClient chatClient) {
        this.chatClient = chatClient;
        registerDefaultAgents();
    }

    private void registerDefaultAgents() {
        register(AgentDefinition.builder()
            .name("architect")
            .role("Solution Architect")
            .systemPrompt("""
                You are a Cloud Foundry solution architect. You design application architectures,
                select appropriate services, define microservice boundaries, and create system
                diagrams. Focus on 12-factor methodology and CF best practices.
                """)
            .capabilities(List.of("architecture-design", "service-selection", "system-design"))
            .build());

        register(AgentDefinition.builder()
            .name("developer")
            .role("Spring Boot Developer")
            .systemPrompt("""
                You are a Spring Boot developer specializing in Cloud Foundry applications.
                You write production-ready code following Spring best practices, including
                proper error handling, testing, and CF service bindings via VCAP_SERVICES.
                """)
            .capabilities(List.of("code-generation", "refactoring", "bug-fixing"))
            .build());

        register(AgentDefinition.builder()
            .name("reviewer")
            .role("Code Reviewer")
            .systemPrompt("""
                You are a senior code reviewer. You review code for security vulnerabilities,
                performance issues, best practice violations, and CF-specific concerns like
                ephemeral filesystem usage, proper health checks, and graceful shutdown.
                """)
            .capabilities(List.of("code-review", "security-audit", "performance-review"))
            .build());

        register(AgentDefinition.builder()
            .name("deployer")
            .role("Deployment Specialist")
            .systemPrompt("""
                You are a CF deployment specialist. You create manifests, configure buildpacks,
                set up service bindings, design blue-green and canary deployment strategies,
                and troubleshoot deployment failures.
                """)
            .capabilities(List.of("manifest-generation", "deployment-strategy", "troubleshooting"))
            .build());
    }

    public void register(AgentDefinition agent) {
        registeredAgents.put(agent.getName(), agent);
        log.info("Registered agent: {} ({})", agent.getName(), agent.getRole());
    }

    public Collection<AgentDefinition> listAgents() {
        return registeredAgents.values();
    }

    /**
     * Execute a multi-agent workflow for a complex task.
     */
    public AgentWorkflowResult executeWorkflow(String taskDescription, String projectContext,
                                                 List<String> agentNames) {
        long start = System.currentTimeMillis();
        List<AgentStepResult> steps = new ArrayList<>();
        StringBuilder sharedContext = new StringBuilder();
        sharedContext.append("PROJECT CONTEXT:\n").append(projectContext).append("\n\n");
        sharedContext.append("TASK:\n").append(taskDescription).append("\n\n");

        for (String agentName : agentNames) {
            AgentDefinition agent = registeredAgents.get(agentName);
            if (agent == null) {
                log.warn("Agent not found: {}", agentName);
                continue;
            }

            log.info("Executing agent: {} for task", agent.getName());

            String prompt = String.format("""
                %s

                Previous agent outputs (shared context):
                %s

                Now provide your contribution for this task based on your role as %s.
                """, taskDescription, sharedContext.toString(), agent.getRole());

            String result = chatClient.prompt()
                .system(agent.getSystemPrompt())
                .user(prompt)
                .call()
                .content();

            sharedContext.append(agent.getRole()).append(" OUTPUT:\n").append(result).append("\n\n");

            steps.add(AgentStepResult.builder()
                .agentName(agent.getName())
                .role(agent.getRole())
                .output(result)
                .timestamp(Instant.now())
                .build());
        }

        return AgentWorkflowResult.builder()
            .taskDescription(taskDescription)
            .steps(steps)
            .totalAgents(agentNames.size())
            .durationMs(System.currentTimeMillis() - start)
            .build();
    }

    /**
     * Stream a single agent's response.
     */
    public Flux<String> streamAgent(String agentName, String userMessage, String context) {
        AgentDefinition agent = registeredAgents.get(agentName);
        if (agent == null) {
            return Flux.just("Agent not found: " + agentName);
        }

        String prompt = context != null && !context.isBlank()
            ? "Context:\n" + context + "\n\nRequest:\n" + userMessage
            : userMessage;

        return chatClient.prompt()
            .system(agent.getSystemPrompt())
            .user(prompt)
            .stream()
            .content();
    }

    @Data
    @lombok.Builder
    public static class AgentDefinition {
        private String name;
        private String role;
        private String systemPrompt;
        private List<String> capabilities;
    }

    @Data
    @lombok.Builder
    public static class AgentStepResult {
        private String agentName;
        private String role;
        private String output;
        private Instant timestamp;
    }

    @Data
    @lombok.Builder
    public static class AgentWorkflowResult {
        private String taskDescription;
        private List<AgentStepResult> steps;
        private int totalAgents;
        private long durationMs;
    }
}
