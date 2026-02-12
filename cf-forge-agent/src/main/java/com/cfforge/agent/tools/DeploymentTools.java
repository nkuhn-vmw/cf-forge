package com.cfforge.agent.tools;

import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.DeployEnvironment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.repository.DeploymentRepository;
import com.cfforge.common.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DeploymentTools {

    private static final Logger log = LoggerFactory.getLogger(DeploymentTools.class);
    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;

    public DeploymentTools(DeploymentRepository deploymentRepository, ProjectRepository projectRepository) {
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
    }

    @Tool(description = "List all deployments for a project, ordered by most recent first. " +
                        "Shows deployment ID, status, environment, strategy, and timestamps.")
    public String listDeployments(@ToolParam(description = "Project UUID") String projectId) {
        try {
            List<Deployment> deployments = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(
                UUID.fromString(projectId));
            if (deployments.isEmpty()) {
                return "No deployments found for project " + projectId;
            }
            return deployments.stream()
                .map(d -> String.format("- ID: %s | Status: %s | Env: %s | Strategy: %s | URL: %s | Created: %s",
                    d.getId(), d.getStatus(), d.getEnvironment(), d.getStrategy(),
                    d.getDeploymentUrl() != null ? d.getDeploymentUrl() : "n/a",
                    d.getCreatedAt()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to list deployments: {}", e.getMessage(), e);
            return "Failed to list deployments: " + e.getMessage();
        }
    }

    @Tool(description = "Get detailed status of a specific deployment including error messages and duration")
    public String getDeploymentStatus(@ToolParam(description = "Deployment UUID") String deploymentId) {
        try {
            return deploymentRepository.findById(UUID.fromString(deploymentId))
                .map(d -> String.format(
                    "Deployment: %s\nProject: %s\nStatus: %s\nEnvironment: %s\n" +
                    "Strategy: %s\nCF App GUID: %s\nURL: %s\nDuration: %s\n" +
                    "Error: %s\nCreated: %s\nUpdated: %s",
                    d.getId(),
                    d.getProject() != null ? d.getProject().getName() : "unknown",
                    d.getStatus(), d.getEnvironment(), d.getStrategy(),
                    d.getCfAppGuid() != null ? d.getCfAppGuid() : "n/a",
                    d.getDeploymentUrl() != null ? d.getDeploymentUrl() : "n/a",
                    d.getDurationMs() != null ? d.getDurationMs() + "ms" : "n/a",
                    d.getErrorMessage() != null ? d.getErrorMessage() : "none",
                    d.getCreatedAt(), d.getUpdatedAt()))
                .orElse("Deployment not found: " + deploymentId);
        } catch (Exception e) {
            log.error("Failed to get deployment status: {}", e.getMessage(), e);
            return "Failed to get deployment status: " + e.getMessage();
        }
    }

    @Tool(description = "Rollback a project to its last successful deployment by creating a new deployment " +
                        "record with ROLLED_BACK status on the failed deployment")
    public String rollbackDeployment(
            @ToolParam(description = "Project UUID") String projectId,
            @ToolParam(description = "Environment to rollback: STAGING or PRODUCTION") String environment) {
        try {
            List<Deployment> deployments = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(
                UUID.fromString(projectId));

            DeployEnvironment env = DeployEnvironment.valueOf(environment.toUpperCase());

            // Find the most recent deployment in the target environment
            var currentDeployment = deployments.stream()
                .filter(d -> d.getEnvironment() == env)
                .findFirst();

            if (currentDeployment.isEmpty()) {
                return "No deployments found in " + environment + " for project " + projectId;
            }

            Deployment current = currentDeployment.get();
            if (current.getStatus() != DeployStatus.DEPLOYED && current.getStatus() != DeployStatus.FAILED) {
                return "Cannot rollback: current deployment status is " + current.getStatus() +
                       ". Only DEPLOYED or FAILED deployments can be rolled back.";
            }

            // Find the last successful deployment before the current one
            var previousSuccess = deployments.stream()
                .filter(d -> d.getEnvironment() == env)
                .filter(d -> d.getStatus() == DeployStatus.DEPLOYED)
                .filter(d -> d.getCreatedAt().isBefore(current.getCreatedAt()))
                .findFirst();

            // Mark current deployment as rolled back
            current.setStatus(DeployStatus.ROLLED_BACK);
            current.setUpdatedAt(Instant.now());
            deploymentRepository.save(current);

            if (previousSuccess.isPresent()) {
                return String.format("Rollback initiated. Deployment %s marked as ROLLED_BACK.\n" +
                    "Previous successful deployment: %s (URL: %s)",
                    current.getId(), previousSuccess.get().getId(),
                    previousSuccess.get().getDeploymentUrl() != null ?
                        previousSuccess.get().getDeploymentUrl() : "n/a");
            } else {
                return String.format("Deployment %s marked as ROLLED_BACK. " +
                    "No previous successful deployment found in %s — manual intervention may be needed.",
                    current.getId(), environment);
            }
        } catch (Exception e) {
            log.error("Failed to rollback deployment: {}", e.getMessage(), e);
            return "Failed to rollback deployment: " + e.getMessage();
        }
    }
}
