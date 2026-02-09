package com.cfforge.agent.evaluation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Spring AI Bench Evaluation Suite (ECO-009).
 *
 * Evaluates the quality of AI-generated code across multiple dimensions:
 * - Correctness: Does the code compile and produce expected results?
 * - CF Compliance: Does it follow CF best practices (12-factor, VCAP, health)?
 * - Security: Are there hardcoded secrets, injection vulnerabilities?
 * - Performance: Are there obvious performance anti-patterns?
 * - Completeness: Is the code production-ready or just a skeleton?
 *
 * Runs automated benchmarks against a set of standard prompts and evaluates
 * the generated output against a rubric.
 */
@Service
@Slf4j
public class AiBenchEvaluator {

    private final ChatClient chatClient;

    private static final String EVALUATOR_PROMPT = """
        You are a code quality evaluator for Cloud Foundry applications. Score the following
        generated code on each dimension from 0-10:

        1. CORRECTNESS: Does the code compile? Are there syntax errors? Logic bugs?
        2. CF_COMPLIANCE: Does it use VCAP_SERVICES? Health endpoints? Proper buildpack config?
        3. SECURITY: Any hardcoded secrets? SQL injection? XSS? Command injection?
        4. PERFORMANCE: N+1 queries? Unbounded collections? Missing pagination?
        5. COMPLETENESS: Is it production-ready? Or a skeleton with TODOs?

        Respond with ONLY a JSON object in this format:
        {
          "correctness": <0-10>,
          "cfCompliance": <0-10>,
          "security": <0-10>,
          "performance": <0-10>,
          "completeness": <0-10>,
          "overallScore": <0-10>,
          "issues": ["issue1", "issue2"],
          "strengths": ["strength1", "strength2"]
        }
        """;

    private static final List<BenchmarkPrompt> STANDARD_BENCHMARKS = List.of(
        new BenchmarkPrompt("rest-api",
            "Generate a Spring Boot REST API with CRUD operations for a Product entity, " +
            "connected to PostgreSQL via CF service binding"),
        new BenchmarkPrompt("auth-api",
            "Generate a Spring Boot API with OAuth2/SSO authentication using CF p-identity service"),
        new BenchmarkPrompt("async-worker",
            "Generate a Spring Boot worker that consumes messages from RabbitMQ with retry and DLQ"),
        new BenchmarkPrompt("react-frontend",
            "Generate a React SPA that calls a Spring Boot API and deploys to CF with staticfile buildpack"),
        new BenchmarkPrompt("microservice",
            "Generate a Spring Boot microservice with Redis caching, health checks, and CF manifest")
    );

    public AiBenchEvaluator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Evaluate a single piece of generated code.
     */
    public EvaluationResult evaluate(String generatedCode, String originalPrompt) {
        String evalPrompt = String.format("""
            Original prompt: %s

            Generated code:
            ```
            %s
            ```

            Evaluate this code.
            """, originalPrompt, generatedCode);

        try {
            String response = chatClient.prompt()
                .system(EVALUATOR_PROMPT)
                .user(evalPrompt)
                .call()
                .content();

            return parseEvaluation(response, originalPrompt);
        } catch (Exception e) {
            log.error("Evaluation failed: {}", e.getMessage());
            return EvaluationResult.builder()
                .prompt(originalPrompt)
                .overallScore(0)
                .error(e.getMessage())
                .evaluatedAt(Instant.now())
                .build();
        }
    }

    /**
     * Run the full benchmark suite.
     */
    public BenchmarkReport runFullBenchmark() {
        log.info("Starting AI Bench evaluation suite ({} benchmarks)", STANDARD_BENCHMARKS.size());
        long start = System.currentTimeMillis();
        List<EvaluationResult> results = new ArrayList<>();

        for (BenchmarkPrompt benchmark : STANDARD_BENCHMARKS) {
            log.info("Running benchmark: {}", benchmark.name());

            // Generate code
            String generated = chatClient.prompt()
                .user(benchmark.prompt())
                .call()
                .content();

            // Evaluate it
            EvaluationResult result = evaluate(generated, benchmark.prompt());
            result.setBenchmarkName(benchmark.name());
            results.add(result);

            log.info("Benchmark {}: score={}/10", benchmark.name(), result.getOverallScore());
        }

        // Aggregate scores
        double avgScore = results.stream()
            .mapToDouble(EvaluationResult::getOverallScore)
            .average()
            .orElse(0);

        return BenchmarkReport.builder()
            .runId(UUID.randomUUID().toString())
            .evaluations(results)
            .averageScore(Math.round(avgScore * 10) / 10.0)
            .totalBenchmarks(STANDARD_BENCHMARKS.size())
            .durationMs(System.currentTimeMillis() - start)
            .runAt(Instant.now())
            .build();
    }

    /**
     * Get the list of standard benchmark prompts.
     */
    public List<BenchmarkPrompt> getStandardBenchmarks() {
        return STANDARD_BENCHMARKS;
    }

    private EvaluationResult parseEvaluation(String response, String prompt) {
        var builder = EvaluationResult.builder()
            .prompt(prompt)
            .rawEvaluation(response)
            .evaluatedAt(Instant.now());

        try {
            // Simple JSON extraction - find the JSON object in the response
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);
                // Parse manually to avoid Jackson dependency
                builder.overallScore(extractScore(json, "overallScore"));
                builder.correctnessScore(extractScore(json, "correctness"));
                builder.cfComplianceScore(extractScore(json, "cfCompliance"));
                builder.securityScore(extractScore(json, "security"));
                builder.performanceScore(extractScore(json, "performance"));
                builder.completenessScore(extractScore(json, "completeness"));
            }
        } catch (Exception e) {
            log.warn("Failed to parse evaluation response: {}", e.getMessage());
            builder.error("Parse error: " + e.getMessage());
        }

        return builder.build();
    }

    private double extractScore(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return 0;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '.') num.append(c);
            else if (!num.isEmpty()) break;
        }
        return num.isEmpty() ? 0 : Double.parseDouble(num.toString());
    }

    public record BenchmarkPrompt(String name, String prompt) {}

    @Data
    @lombok.Builder
    public static class EvaluationResult {
        private String benchmarkName;
        private String prompt;
        private double overallScore;
        private double correctnessScore;
        private double cfComplianceScore;
        private double securityScore;
        private double performanceScore;
        private double completenessScore;
        private String rawEvaluation;
        private String error;
        private Instant evaluatedAt;
    }

    @Data
    @lombok.Builder
    public static class BenchmarkReport {
        private String runId;
        private List<EvaluationResult> evaluations;
        private double averageScore;
        private int totalBenchmarks;
        private long durationMs;
        private Instant runAt;
    }
}
