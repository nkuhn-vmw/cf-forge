package com.cfforge.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AutoscalerPolicy(
    @JsonProperty("instance_min_count") int instanceMinCount,
    @JsonProperty("instance_max_count") int instanceMaxCount,
    @JsonProperty("scaling_rules") List<ScalingRule> scalingRules
) {
    public record ScalingRule(
        @JsonProperty("metric_type") String metricType,
        @JsonProperty("breach_duration_secs") int breachDurationSecs,
        int threshold,
        String operator,
        @JsonProperty("cool_down_secs") int coolDownSecs,
        String adjustment
    ) {}
}
