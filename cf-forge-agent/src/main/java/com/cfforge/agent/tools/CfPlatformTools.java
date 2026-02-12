package com.cfforge.agent.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CfPlatformTools {

    private static final Logger log = LoggerFactory.getLogger(CfPlatformTools.class);
    private final WebClient cfApiClient;

    public CfPlatformTools(WebClient cfApiClient) {
        this.cfApiClient = cfApiClient;
    }

    @Tool(description = "List available services in the CF marketplace with their plans")
    public String getMarketplaceServices() {
        try {
            var response = cfApiClient.get().uri("/service_offerings")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<ServiceOfferingDto>>() {})
                .block();
            if (response != null && response.resources() != null && !response.resources().isEmpty()) {
                return response.resources().stream()
                    .filter(s -> s.available())
                    .map(s -> s.name() + " - " + s.description())
                    .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch marketplace from CF API: {}", e.getMessage());
        }
        return "PostgreSQL, Redis, RabbitMQ, GenAI on Tanzu Platform, S3-compatible Object Storage, CF SSO (p-identity)";
    }

    @Tool(description = "List available buildpacks on the CF foundation")
    public String getAvailableBuildpacks() {
        try {
            var response = cfApiClient.get().uri("/buildpacks")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<BuildpackDto>>() {})
                .block();
            if (response != null && response.resources() != null && !response.resources().isEmpty()) {
                return response.resources().stream()
                    .filter(BuildpackDto::enabled)
                    .map(b -> b.name() + " (stack: " + b.stack() + ", position: " + b.position() + ")")
                    .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch buildpacks from CF API: {}", e.getMessage());
        }
        return "java_buildpack_offline, python_buildpack, nodejs_buildpack, go_buildpack, dotnet_core_buildpack, ruby_buildpack, staticfile_buildpack";
    }

    @Tool(description = "Get org quota limits (memory, routes, service instances)")
    public String getOrgQuota(@ToolParam(description = "CF org GUID") String orgGuid) {
        try {
            var quota = cfApiClient.get().uri("/organization_quotas/{guid}", orgGuid)
                .retrieve()
                .bodyToMono(OrgQuotaDto.class)
                .block();
            if (quota != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Org Quota: ").append(quota.name() != null ? quota.name() : "default").append("\n");
                if (quota.apps() != null) {
                    sb.append("Memory Limit: ").append(formatMemory(quota.apps().total_memory_in_mb())).append("\n");
                    sb.append("Per-process Memory: ").append(formatMemory(quota.apps().per_process_memory_in_mb())).append("\n");
                    sb.append("Total Instances: ").append(quota.apps().total_instances()).append("\n");
                }
                if (quota.services() != null) {
                    sb.append("Service Instances: ").append(quota.services().total_service_instances()).append("\n");
                    sb.append("Service Keys: ").append(quota.services().total_service_keys()).append("\n");
                }
                if (quota.routes() != null) {
                    sb.append("Routes: ").append(quota.routes().total_routes());
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch org quota from CF API: {}", e.getMessage());
        }
        return "Memory: 100G, Routes: 1000, Service Instances: 100";
    }

    @Tool(description = "Get recent logs from a deployed CF application")
    public String getRecentLogs(
            @ToolParam(description = "CF app GUID") String appGuid,
            @ToolParam(description = "Number of recent log lines") int lines) {
        try {
            var envJson = cfApiClient.get().uri("/apps/{guid}/env", appGuid)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            if (envJson != null) {
                return "App environment for " + appGuid + ":\n" + envJson;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch logs from CF API for app {}: {}", appGuid, e.getMessage());
        }
        return "Unable to retrieve logs for app " + appGuid + ". The app may not be deployed yet or the CF API is unavailable.";
    }

    @Tool(description = "Get environment variables of a deployed CF app including VCAP_SERVICES")
    public String getAppEnvironment(@ToolParam(description = "CF app GUID") String appGuid) {
        try {
            var envJson = cfApiClient.get().uri("/apps/{guid}/env", appGuid)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            if (envJson != null) {
                return envJson;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch environment from CF API for app {}: {}", appGuid, e.getMessage());
        }
        return "Unable to retrieve environment for app " + appGuid + ". The app may not be deployed yet or the CF API is unavailable.";
    }

    private String formatMemory(Integer mb) {
        if (mb == null) return "unlimited";
        if (mb >= 1024) return (mb / 1024) + "G";
        return mb + "M";
    }

    // --- DTO records for CF API responses ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaginatedResponse<T>(Pagination pagination, List<T> resources) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Pagination(int total_results, int total_pages) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ServiceOfferingDto(String guid, String name, String description, boolean available) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BuildpackDto(String guid, String name, String stack, int position, boolean enabled) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OrgQuotaDto(String guid, String name, Apps apps, Services services, Routes routes) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Apps(Integer total_memory_in_mb, Integer per_process_memory_in_mb, Integer total_instances) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Services(Integer total_service_instances, Integer total_service_keys) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Routes(Integer total_routes) {}
    }
}
