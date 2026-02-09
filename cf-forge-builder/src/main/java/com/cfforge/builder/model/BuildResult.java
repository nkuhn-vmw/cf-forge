package com.cfforge.builder.model;

import com.cfforge.common.enums.BuildStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuildResult {
    private BuildStatus status;
    private String log;
    private String artifactPath;
    private long durationMs;
    private String errorMessage;
}
