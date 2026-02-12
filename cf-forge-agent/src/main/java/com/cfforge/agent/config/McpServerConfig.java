package com.cfforge.agent.config;

import com.cfforge.agent.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider cfForgeTools(
            CfPlatformTools cfPlatformTools,
            WorkspaceTools workspaceTools,
            BuildDeployTools buildDeployTools,
            DocumentationTools documentationTools,
            ProjectTools projectTools,
            DeploymentTools deploymentTools,
            ServiceTools serviceTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(cfPlatformTools, workspaceTools, buildDeployTools,
                        documentationTools, projectTools, deploymentTools, serviceTools)
            .build();
    }
}
