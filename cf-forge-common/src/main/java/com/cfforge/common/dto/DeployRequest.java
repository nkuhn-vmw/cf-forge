package com.cfforge.common.dto;

import com.cfforge.common.enums.DeployEnvironment;
import java.util.UUID;

public record DeployRequest(UUID projectId, DeployEnvironment environment) {}
