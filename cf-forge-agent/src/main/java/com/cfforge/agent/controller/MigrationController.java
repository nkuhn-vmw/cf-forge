package com.cfforge.agent.controller;

import com.cfforge.agent.service.MigrationAssistantService;
import com.cfforge.agent.service.MigrationAssistantService.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the AI migration assistant.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

    private final MigrationAssistantService migrationService;

    public MigrationController(MigrationAssistantService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/analyze")
    public MigrationPlan analyze(@RequestBody Map<String, String> request) {
        return migrationService.analyzeLegacyCode(
            request.getOrDefault("code", ""),
            request.getOrDefault("description", "Legacy application"),
            request.getOrDefault("sourceStack", "unknown")
        );
    }

    @PostMapping("/dependencies")
    public DependencyAnalysis analyzeDependencies(@RequestBody Map<String, String> request) {
        return migrationService.analyzeDependencies(
            request.get("buildConfig"),
            request.getOrDefault("type", "maven")
        );
    }

    @PostMapping("/manifest")
    public Map<String, String> generateManifest(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> services = (List<String>) request.getOrDefault("services", List.of());
        String manifest = migrationService.generateManifest(
            (String) request.getOrDefault("appName", "migrated-app"),
            (String) request.getOrDefault("sourceStack", "j2ee"),
            services,
            (String) request.getOrDefault("memory", "1G"),
            (int) request.getOrDefault("instances", 2)
        );
        return Map.of("manifest", manifest);
    }
}
