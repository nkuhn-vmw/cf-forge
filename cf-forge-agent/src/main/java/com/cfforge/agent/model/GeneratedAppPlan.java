package com.cfforge.agent.model;

import java.util.List;
import java.util.Map;

public record GeneratedAppPlan(
    String appName,
    String language,
    String framework,
    String buildpack,
    String memory,
    String diskQuota,
    int instances,
    HealthCheck healthCheck,
    List<ServiceRequirement> services,
    List<String> routes,
    Map<String, String> envVars,
    List<GeneratedFile> files
) {
    public record HealthCheck(String type, String httpEndpoint) {}
    public record ServiceRequirement(String name, String type, String plan) {}
}
