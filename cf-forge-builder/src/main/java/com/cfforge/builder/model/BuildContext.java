package com.cfforge.builder.model;

import com.cfforge.common.enums.Language;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.UUID;

@Data
@Builder
public class BuildContext {
    private UUID projectId;
    private UUID buildId;
    private Path workDir;
    private Language language;
    private String framework;
}
