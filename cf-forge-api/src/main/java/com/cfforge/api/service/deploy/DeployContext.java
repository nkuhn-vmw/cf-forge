package com.cfforge.api.service.deploy;

import com.cfforge.common.entity.Project;
import com.cfforge.common.enums.DeployEnvironment;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DeployContext {
    private UUID projectId;
    private UUID deploymentId;
    private String appName;
    private String appGuid;
    private String manifestYaml;
    private String artifactPath;
    private DeployEnvironment environment;
    private String domain;
    private Map<String, String> envVars;
}
