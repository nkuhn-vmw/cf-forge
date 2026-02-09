package com.cfforge.agent.model;

import java.util.List;

public record ErrorFix(
    String errorSummary,
    String rootCause,
    List<SuggestedFix> fixes,
    boolean autoFixable
) {
    public record SuggestedFix(
        String description,
        String filePath,
        String originalCode,
        String fixedCode,
        FixConfidence confidence
    ) {}

    public enum FixConfidence { HIGH, MEDIUM, LOW }
}
