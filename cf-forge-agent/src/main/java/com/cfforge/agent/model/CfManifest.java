package com.cfforge.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record CfManifest(List<CfApplication> applications) {

    public record CfApplication(
        String name,
        String memory,
        @JsonProperty("disk_quota") String diskQuota,
        int instances,
        String buildpack,
        List<String> buildpacks,
        String path,
        String command,
        @JsonProperty("health_check_type") String healthCheckType,
        @JsonProperty("health_check_http_endpoint") String healthCheckHttpEndpoint,
        @JsonProperty("health_check_invocation_timeout") Integer healthCheckInvocationTimeout,
        Integer timeout,
        @JsonProperty("no_route") Boolean noRoute,
        @JsonProperty("random_route") Boolean randomRoute,
        List<RouteEntry> routes,
        List<String> services,
        Map<String, String> env,
        String stack,
        List<CfProcess> processes,
        List<CfSidecar> sidecars
    ) {}

    public record RouteEntry(String route) {}
    public record CfProcess(String type, String command, String memory, int instances) {}
    public record CfSidecar(String name, String command, @JsonProperty("process_types") List<String> processTypes) {}
}
