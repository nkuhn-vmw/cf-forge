package com.cfforge.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CfPlatformTools {

    @Tool(description = "List available services in the CF marketplace with their plans")
    public String getMarketplaceServices() {
        return "PostgreSQL, Redis, RabbitMQ, GenAI on Tanzu Platform, S3-compatible Object Storage, CF SSO (p-identity)";
    }

    @Tool(description = "List available buildpacks on the CF foundation")
    public String getAvailableBuildpacks() {
        return "java_buildpack_offline, python_buildpack, nodejs_buildpack, go_buildpack, dotnet_core_buildpack, ruby_buildpack, staticfile_buildpack";
    }

    @Tool(description = "Get org quota limits (memory, routes, service instances)")
    public String getOrgQuota(@ToolParam(description = "CF org GUID") String orgGuid) {
        return "Memory: 100G, Routes: 1000, Service Instances: 100";
    }

    @Tool(description = "Get recent logs from a deployed CF application")
    public String getRecentLogs(
            @ToolParam(description = "CF app GUID") String appGuid,
            @ToolParam(description = "Number of recent log lines") int lines) {
        return "Log retrieval requires CF API connection. App GUID: " + appGuid;
    }

    @Tool(description = "Get environment variables of a deployed CF app including VCAP_SERVICES")
    public String getAppEnvironment(@ToolParam(description = "CF app GUID") String appGuid) {
        return "Environment retrieval requires CF API connection. App GUID: " + appGuid;
    }
}
