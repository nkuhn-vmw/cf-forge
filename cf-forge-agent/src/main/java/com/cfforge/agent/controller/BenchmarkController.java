package com.cfforge.agent.controller;

import com.cfforge.agent.evaluation.AiBenchEvaluator;
import com.cfforge.agent.evaluation.AiBenchEvaluator.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the AI Bench evaluation suite.
 */
@RestController
@RequestMapping("/api/v1/bench")
public class BenchmarkController {

    private final AiBenchEvaluator evaluator;

    public BenchmarkController(AiBenchEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @GetMapping("/benchmarks")
    public List<BenchmarkPrompt> listBenchmarks() {
        return evaluator.getStandardBenchmarks();
    }

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@RequestBody Map<String, String> request) {
        return evaluator.evaluate(
            request.get("code"),
            request.getOrDefault("prompt", "Code evaluation")
        );
    }

    @PostMapping("/run")
    public BenchmarkReport runFullBenchmark() {
        return evaluator.runFullBenchmark();
    }
}
