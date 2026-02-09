package com.cfforge.agent.model;

import java.util.List;

public record EvaluationResult(
    int correctnessScore,
    int cfBestPracticesScore,
    int securityScore,
    int productionReadinessScore,
    int codeQualityScore,
    double overallScore,
    String summary,
    List<String> improvements
) {}
