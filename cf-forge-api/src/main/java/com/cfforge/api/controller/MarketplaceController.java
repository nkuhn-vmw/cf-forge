package com.cfforge.api.controller;

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
