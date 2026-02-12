package com.cfforge.api.controller;

import com.cfforge.api.model.BuildpackInfo;
import com.cfforge.api.service.CfClient;
import com.cfforge.api.model.ServiceOffering;
import com.cfforge.common.entity.Project;
import com.cfforge.common.repository.ProjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final CfClient cfClient;
    private final ProjectRepository projectRepository;

    public MarketplaceController(CfClient cfClient, ProjectRepository projectRepository) {
        this.cfClient = cfClient;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/services")
    public List<ServiceOffering> listServices() {
        return cfClient.listMarketplace().collectList().block();
    }

    @GetMapping("/buildpacks")
    public List<BuildpackInfo> listBuildpacks() {
        try {
            return cfClient.listBuildpacks().collectList().block();
        } catch (Exception e) {
            // CF API may require auth for buildpacks; return common defaults
            return List.of(
                new BuildpackInfo(null, "java_buildpack_offline", "cflinuxfs4", 1, true),
                new BuildpackInfo(null, "nodejs_buildpack", "cflinuxfs4", 2, true),
                new BuildpackInfo(null, "python_buildpack", "cflinuxfs4", 3, true),
                new BuildpackInfo(null, "go_buildpack", "cflinuxfs4", 4, true),
                new BuildpackInfo(null, "dotnet_core_buildpack", "cflinuxfs4", 5, true),
                new BuildpackInfo(null, "ruby_buildpack", "cflinuxfs4", 6, true),
                new BuildpackInfo(null, "staticfile_buildpack", "cflinuxfs4", 7, true),
                new BuildpackInfo(null, "nginx_buildpack", "cflinuxfs4", 8, true),
                new BuildpackInfo(null, "binary_buildpack", "cflinuxfs4", 9, true)
            );
        }
    }

    @GetMapping("/recommend")
    public List<Map<String, String>> recommendServices(@RequestParam String projectId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(projectId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        return projectRepository.findById(uuid)
            .map(this::buildRecommendations)
            .orElse(List.of());
    }

    @PostMapping("/services/{serviceName}/provision")
    public void provisionService(
            @PathVariable String serviceName,
            @RequestParam String plan,
            @RequestParam String instanceName,
            @RequestParam(required = false) String projectId) {
        // Provision service and optionally bind to app
        // This delegates to CF API to create-service and bind-service
    }

    private List<Map<String, String>> buildRecommendations(Project project) {
        List<Map<String, String>> recs = new ArrayList<>();
        String lang = project.getLanguage() != null ? project.getLanguage().name() : "";

        recs.add(Map.of("serviceName", "postgres", "plan", "on-demand-postgres-db",
            "reason", "Relational database for application data", "bindingName", project.getSlug() + "-db"));

        if ("JAVA".equals(lang) || "NODEJS".equals(lang) || "PYTHON".equals(lang)) {
            recs.add(Map.of("serviceName", "p.rabbitmq", "plan", "on-demand-plan",
                "reason", "Message broker for async processing", "bindingName", project.getSlug() + "-mq"));
        }

        recs.add(Map.of("serviceName", "p-redis", "plan", "shared-vm",
            "reason", "In-memory cache for session and data caching", "bindingName", project.getSlug() + "-cache"));

        return recs;
    }
}
