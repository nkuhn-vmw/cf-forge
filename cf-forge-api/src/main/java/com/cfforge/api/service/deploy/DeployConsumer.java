package com.cfforge.api.service.deploy;

import com.cfforge.common.dto.DeployRequest;
import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.enums.DeployStrategy;
import com.cfforge.common.events.MetricEventPublisher;
import com.cfforge.common.repository.DeploymentRepository;
import com.cfforge.common.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.List;

@Component
@Slf4j
public class DeployConsumer {

    private final Map<String, DeployStrategyInterface> strategies;
    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final MetricEventPublisher metricPublisher;

    public DeployConsumer(List<DeployStrategyInterface> strategyList,
                          DeploymentRepository deploymentRepository,
                          ProjectRepository projectRepository,
                          MetricEventPublisher metricPublisher) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(DeployStrategyInterface::strategyName, s -> s));
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
        this.metricPublisher = metricPublisher;
    }

    @Bean
    public Consumer<DeployRequest> deployRequest() {
        return request -> {
            log.info("Processing deploy request for project: {}", request.projectId());

            var project = projectRepository.findById(request.projectId()).orElseThrow();
            var deployment = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(request.projectId())
                .stream().findFirst().orElseThrow();

            deployment.setStatus(DeployStatus.IN_PROGRESS);
            deploymentRepository.save(deployment);

            long start = System.currentTimeMillis();

            try {
                DeployStrategyInterface strategy = strategies.getOrDefault(
                    deployment.getStrategy().name(),
                    strategies.get("ROLLING")
                );

                var ctx = DeployContext.builder()
                    .projectId(request.projectId())
                    .deploymentId(deployment.getId())
                    .appName(project.getSlug())
                    .environment(request.environment())
                    .domain("apps.cf.example.com")
                    .build();

                DeployResult result = strategy.execute(ctx);
                long duration = System.currentTimeMillis() - start;

                deployment.setStatus(result.getStatus());
                deployment.setDeploymentUrl(result.getDeploymentUrl());
                deploymentRepository.save(deployment);

                metricPublisher.publishSuccess("deploy.completed", null, request.projectId(), (int) duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                deployment.setStatus(DeployStatus.FAILED);
                deploymentRepository.save(deployment);
                metricPublisher.publishFailure("deploy.failed", null, request.projectId(), e.getMessage());
                log.error("Deploy failed for project: {}", request.projectId(), e);
            }
        };
    }
}
