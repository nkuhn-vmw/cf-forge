package com.cfforge.api.service.deploy;

import com.cfforge.common.entity.Deployment;
import com.cfforge.common.entity.Project;

public interface DeployStrategyInterface {
    DeployResult execute(DeployContext ctx);
    void rollback(DeployContext ctx);
    String strategyName();
}
