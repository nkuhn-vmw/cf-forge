package com.cfforge.api.service.deploy;

import com.cfforge.common.enums.DeployStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeployResult {
    private DeployStatus status;
    private String deploymentUrl;
    private String log;
    private long durationMs;
    private String errorMessage;
}
