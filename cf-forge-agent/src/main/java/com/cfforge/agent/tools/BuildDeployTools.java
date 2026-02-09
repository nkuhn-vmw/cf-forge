package com.cfforge.agent.tools;

import com.cfforge.common.dto.BuildRequest;
import com.cfforge.common.dto.DeployRequest;
import com.cfforge.common.enums.DeployEnvironment;
import com.cfforge.common.enums.TriggerType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BuildDeployTools {

    private final StreamBridge streamBridge;

    public BuildDeployTools(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Tool(description = "Trigger a build for the project (compile, test, scan, package)")
    public String triggerBuild(@ToolParam("Project ID") String projectId) {
        streamBridge.send("buildRequest-out-0",
            new BuildRequest(UUID.fromString(projectId), TriggerType.AGENT));
        return "Build triggered for project " + projectId;
    }

    @Tool(description = "Deploy the project to a CF space (staging or production)")
    public String triggerDeploy(
            @ToolParam("Project ID") String projectId,
            @ToolParam("Environment: staging or production") String environment) {
        streamBridge.send("deployRequest-out-0",
            new DeployRequest(UUID.fromString(projectId), DeployEnvironment.valueOf(environment.toUpperCase())));
        return "Deployment triggered to " + environment;
    }
}
