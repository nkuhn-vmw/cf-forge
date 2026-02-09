package com.cfforge.builder.pipeline;

import com.cfforge.builder.model.BuildContext;
import com.cfforge.builder.model.BuildResult;
import com.cfforge.common.enums.Language;

public interface BuildPipeline {
    BuildResult execute(BuildContext context);
    Language supportedLanguage();
}
