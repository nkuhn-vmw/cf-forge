package com.cfforge.agent.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI Migration Assistant (ECO-004).
 *
 * Analyzes legacy application codebases and generates comprehensive migration
 * plans to Cloud Foundry + Spring Boot. Detects the source technology stack,
 * identifies compatibility issues, recommends CF services, and generates
 * a step-by-step migration roadmap.
 */
@Service
@Slf4j
public class MigrationAssistantService {

    private final ChatClient chatClient;

    private static final String MIGRATION_SYSTEM_PROMPT = """
        You are a Cloud Foundry migration expert. Your job is to analyze legacy applications
        and create detailed migration plans to modernize them onto Cloud Foundry with Spring Boot.

        When analyzing code, identify:
        1. Source technology stack (J2EE, .NET, PHP, legacy Spring, etc.)
        2. Data storage patterns (RDBMS, files, sessions)
        3. External integrations and dependencies
        4. Configuration patterns (property files, JNDI, environment)
        5. Deployment model (WAR, EAR, standalone)
        6. Security model (container-managed, custom)

        Then provide:
        - A migration complexity score (LOW, MEDIUM, HIGH, CRITICAL)
        - Step-by-step migration plan
        - CF service recommendations (PostgreSQL, Redis, RabbitMQ, SSO, etc.)
        - Required code changes with examples
        - Estimated effort breakdown
        - Risk assessment and mitigation strategies

        Always recommend Spring Boot 3.4+ with Cloud Foundry best practices:
        - 12-factor app methodology
        - VCAP_SERVICES for service bindings
        - Actuator health endpoints
        - CF buildpack selection
        - Proper memory/disk allocation
        """;

    public MigrationAssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Analyze a codebase and generate a migration plan.
     */
    public MigrationPlan analyzeLegacyCode(String codeSnippets, String description,
                                             String sourceStack) {
        String userPrompt = String.format("""
            Analyze this legacy application for migration to Cloud Foundry + Spring Boot.

            Source Stack: %s
            Description: %s

            Code samples:
            ```
            %s
            ```

            Generate a complete migration plan as structured output.
            """, sourceStack, description, codeSnippets);

        return chatClient.prompt()
            .system(MIGRATION_SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .entity(MigrationPlan.class);
    }

    /**
     * Analyze dependencies and identify CF-compatible alternatives.
     */
    public DependencyAnalysis analyzeDependencies(String buildConfig, String configType) {
        String userPrompt = String.format("""
            Analyze these %s dependencies and identify:
            1. Which are CF-compatible as-is
            2. Which need alternatives on CF
            3. Which can be replaced by CF services
            4. Required Spring Boot starter mappings

            Build configuration:
            ```
            %s
            ```
            """, configType, buildConfig);

        return chatClient.prompt()
            .system(MIGRATION_SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .entity(DependencyAnalysis.class);
    }

    /**
     * Generate a CF manifest for the migrated application.
     */
    public String generateManifest(String appName, String stack, List<String> services,
                                     String memory, int instances) {
        String userPrompt = String.format("""
            Generate a Cloud Foundry manifest.yml for:
            - App name: %s
            - Migrated from: %s
            - Required services: %s
            - Memory: %s
            - Instances: %d

            Include appropriate buildpack, health check, and env vars.
            Return ONLY the manifest.yml content, no explanation.
            """, appName, stack, String.join(", ", services), memory, instances);

        return chatClient.prompt()
            .system(MIGRATION_SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .content();
    }

    // --- DTOs ---

    @Data
    public static class MigrationPlan {
        private String sourceStack;
        private String targetStack;
        private String complexityScore; // LOW, MEDIUM, HIGH, CRITICAL
        private List<MigrationStep> steps;
        private List<String> recommendedServices;
        private List<String> risks;
        private Map<String, String> effortEstimate;
    }

    @Data
    public static class MigrationStep {
        private int order;
        private String title;
        private String description;
        private String category; // "code", "config", "infrastructure", "data", "testing"
        private String effort; // "hours", "days", "weeks"
    }

    @Data
    public static class DependencyAnalysis {
        private List<DependencyMapping> compatible;
        private List<DependencyMapping> needsAlternative;
        private List<DependencyMapping> replacedByService;
        private List<String> springBootStarters;
    }

    @Data
    public static class DependencyMapping {
        private String original;
        private String replacement;
        private String notes;
    }
}
