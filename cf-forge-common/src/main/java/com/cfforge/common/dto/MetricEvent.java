package com.cfforge.common.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MetricEvent(
    String type,
    String source,
    UUID userId,
    UUID projectId,
    Integer durationMs,
    boolean success,
    String errorMessage,
    Map<String, String> dimensions,
    Instant timestamp
) {}
