package com.cfforge.builder.consumer;

import com.cfforge.builder.model.BuildContext;
import com.cfforge.builder.model.BuildResult;
import com.cfforge.builder.pipeline.BuildPipeline;
import com.cfforge.common.dto.BuildRequest;
import com.cfforge.common.entity.Build;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.enums.Language;
import com.cfforge.common.events.MetricEventPublisher;
import com.cfforge.common.repository.BuildRepository;
import com.cfforge.common.repository.ProjectRepository;
import com.cfforge.common.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BuildPipelineConsumer {

    private final List<BuildPipeline> pipelines;
    private final BuildRepository buildRepository;
    private final ProjectRepository projectRepository;
    private final S3StorageService storageService;
    private final MetricEventPublisher metricPublisher;
    private final Map<Language, BuildPipeline> pipelineMap;

    public BuildPipelineConsumer(List<BuildPipeline> pipelines,
                                  BuildRepository buildRepository,
                                  ProjectRepository projectRepository,
                                  S3StorageService storageService,
                                  MetricEventPublisher metricPublisher) {
        this.pipelines = pipelines;
        this.buildRepository = buildRepository;
        this.projectRepository = projectRepository;
        this.storageService = storageService;
        this.metricPublisher = metricPublisher;
        this.pipelineMap = pipelines.stream()
            .collect(Collectors.toMap(BuildPipeline::supportedLanguage, p -> p));
    }

    @Bean
    public Consumer<BuildRequest> buildRequest() {
        return request -> {
            log.info("Received build request for project: {}", request.projectId());
            long startTime = System.currentTimeMillis();

            var project = projectRepository.findById(request.projectId()).orElseThrow();
            var build = buildRepository.findByProjectIdOrderByCreatedAtDesc(request.projectId())
                .stream().findFirst().orElseThrow();

            build.setStatus(BuildStatus.BUILDING);
            buildRepository.save(build);

            try {
                Path workDir = Files.createTempDirectory("cfforge-build-");
                var context = BuildContext.builder()
                    .projectId(request.projectId())
                    .buildId(build.getId())
                    .workDir(workDir)
                    .language(project.getLanguage())
                    .framework(project.getFramework())
                    .build();

                BuildPipeline pipeline = pipelineMap.get(project.getLanguage());
                if (pipeline == null) {
                    throw new RuntimeException("No build pipeline for language: " + project.getLanguage());
                }

                BuildResult result = pipeline.execute(context);
                long duration = System.currentTimeMillis() - startTime;

                build.setStatus(result.getStatus());
                build.setBuildLog(result.getLog());
                build.setArtifactPath(result.getArtifactPath());
                build.setDurationMs((int) duration);
                buildRepository.save(build);

                metricPublisher.publishSuccess("build.completed", null, request.projectId(), (int) duration);
                log.info("Build completed for project: {} in {}ms", request.projectId(), duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                build.setStatus(BuildStatus.FAILED);
                build.setBuildLog(e.getMessage());
                build.setDurationMs((int) duration);
                buildRepository.save(build);

                metricPublisher.publishFailure("build.failed", null, request.projectId(), e.getMessage());
                log.error("Build failed for project: {}", request.projectId(), e);
            }
        };
    }
}
