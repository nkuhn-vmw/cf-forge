package com.cfforge.api.controller;

import com.cfforge.common.dto.DeployRequest;
import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.DeployEnvironment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.repository.DeploymentRepository;
import com.cfforge.common.repository.ProjectRepository;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/deployments")
public class DeploymentController {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final StreamBridge streamBridge;

    public DeploymentController(DeploymentRepository deploymentRepository,
                                 ProjectRepository projectRepository,
                                 StreamBridge streamBridge) {
        this.deploymentRepository = deploymentRepository;
        this.projectRepository = projectRepository;
        this.streamBridge = streamBridge;
    }

    @PostMapping
    public ResponseEntity<Deployment> triggerDeploy(@PathVariable UUID projectId,
                                                     @RequestBody(required = false) Map<String, String> body) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        String env = body != null ? body.getOrDefault("environment", "STAGING") : "STAGING";
        var deployment = Deployment.builder()
            .project(project)
            .environment(DeployEnvironment.valueOf(env))
            .status(DeployStatus.PENDING)
            .build();
        deployment = deploymentRepository.save(deployment);

        streamBridge.send("deployRequest-out-0", new DeployRequest(projectId, DeployEnvironment.valueOf(env)));
        return ResponseEntity.status(HttpStatus.CREATED).body(deployment);
    }

    @GetMapping
    public List<Deployment> listDeployments(@PathVariable UUID projectId) {
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @GetMapping("/{deployId}")
    public ResponseEntity<Deployment> getDeployment(@PathVariable UUID projectId,
                                                     @PathVariable UUID deployId) {
        return deploymentRepository.findById(deployId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
