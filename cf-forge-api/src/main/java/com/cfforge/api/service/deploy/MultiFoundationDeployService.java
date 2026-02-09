package com.cfforge.api.service.deploy;

import com.cfforge.common.entity.CfTarget;
import com.cfforge.common.entity.Project;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.repository.CfTargetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-Foundation Deployment Service (ECO-003).
 *
 * Orchestrates deployments across multiple CF foundations. Each project
 * can be deployed to one or more foundations simultaneously. Tracks
 * deployment status per foundation.
 */
@Service
@Slf4j
public class MultiFoundationDeployService {

    private final CfTargetRepository cfTargetRepository;
    private final Map<String, DeployStrategyInterface> strategies;

    public MultiFoundationDeployService(CfTargetRepository cfTargetRepository,
                                         Map<String, DeployStrategyInterface> strategies) {
        this.cfTargetRepository = cfTargetRepository;
        this.strategies = strategies;
    }

    /**
     * Deploy a project to multiple foundations in parallel.
     */
    public Map<UUID, DeployResult> deployToMultiple(Project project,
                                                      List<UUID> targetIds,
                                                      String strategyName,
                                                      DeployContext baseCtx) {
        Map<UUID, DeployResult> results = new ConcurrentHashMap<>();

        List<CfTarget> targets = targetIds.stream()
            .map(id -> cfTargetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + id)))
            .collect(Collectors.toList());

        DeployStrategyInterface strategy = strategies.getOrDefault(
            strategyName + "Deploy",
            strategies.get("rollingDeploy"));

        // Deploy to each foundation
        targets.parallelStream().forEach(target -> {
            log.info("Deploying {} to foundation {} ({})",
                project.getName(), target.getApiEndpoint(), target.getSpaceName());

            DeployContext ctx = DeployContext.builder()
                .projectId(project.getId())
                .deploymentId(baseCtx.getDeploymentId())
                .appName(baseCtx.getAppName())
                .manifestYaml(baseCtx.getManifestYaml())
                .artifactPath(baseCtx.getArtifactPath())
                .environment(baseCtx.getEnvironment())
                .domain(target.getApiEndpoint().replace("api.", "apps."))
                .envVars(baseCtx.getEnvVars() != null ? baseCtx.getEnvVars() : Map.of())
                .build();

            try {
                DeployResult result = strategy.execute(ctx);
                results.put(target.getId(), result);
                log.info("Deployment to {} complete: {}", target.getApiEndpoint(), result.getStatus());
            } catch (Exception e) {
                log.error("Deployment to {} failed: {}", target.getApiEndpoint(), e.getMessage());
                results.put(target.getId(), DeployResult.builder()
                    .status(DeployStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build());
            }
        });

        return results;
    }

    /**
     * Get all available foundations for deployment.
     */
    public List<Map<String, Object>> getAvailableFoundations() {
        return cfTargetRepository.findAll().stream()
            .map(t -> {
                Map<String, Object> foundation = new LinkedHashMap<>();
                foundation.put("id", t.getId());
                foundation.put("endpoint", t.getApiEndpoint());
                foundation.put("org", t.getOrgName());
                foundation.put("space", t.getSpaceName());
                foundation.put("isDefault", t.isDefault());
                return foundation;
            })
            .collect(Collectors.toList());
    }
}
