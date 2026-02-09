package com.cfforge.api.controller;

import com.cfforge.common.dto.BuildRequest;
import com.cfforge.common.entity.Build;
import com.cfforge.common.enums.TriggerType;
import com.cfforge.common.repository.BuildRepository;
import com.cfforge.common.repository.ProjectRepository;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/builds")
public class BuildController {

    private final BuildRepository buildRepository;
    private final ProjectRepository projectRepository;
    private final StreamBridge streamBridge;

    public BuildController(BuildRepository buildRepository, ProjectRepository projectRepository,
                           StreamBridge streamBridge) {
        this.buildRepository = buildRepository;
        this.projectRepository = projectRepository;
        this.streamBridge = streamBridge;
    }

    @PostMapping
    public ResponseEntity<Build> triggerBuild(@PathVariable UUID projectId) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        var build = Build.builder()
            .project(project)
            .triggerType(TriggerType.MANUAL)
            .build();
        build = buildRepository.save(build);

        streamBridge.send("buildRequest-out-0", new BuildRequest(projectId, TriggerType.MANUAL));
        return ResponseEntity.status(HttpStatus.CREATED).body(build);
    }

    @GetMapping
    public List<Build> listBuilds(@PathVariable UUID projectId) {
        return buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<Build> getBuild(@PathVariable UUID projectId, @PathVariable UUID buildId) {
        return buildRepository.findById(buildId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
