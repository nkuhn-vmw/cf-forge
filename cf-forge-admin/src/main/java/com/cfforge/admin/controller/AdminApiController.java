package com.cfforge.admin.controller;

import com.cfforge.admin.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private final UserMetricsService userMetrics;
    private final BuildMetricsService buildMetrics;
    private final DeploymentMetricsService deploymentMetrics;
    private final AgentMetricsService agentMetrics;
    private final HealthCheckScheduler healthChecker;

    public AdminApiController(UserMetricsService userMetrics, BuildMetricsService buildMetrics,
                               DeploymentMetricsService deploymentMetrics, AgentMetricsService agentMetrics,
                               HealthCheckScheduler healthChecker) {
        this.userMetrics = userMetrics;
        this.buildMetrics = buildMetrics;
        this.deploymentMetrics = deploymentMetrics;
        this.agentMetrics = agentMetrics;
        this.healthChecker = healthChecker;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return Map.of(
            "users", userMetrics.getUserMetrics(since, LocalDateTime.now()),
            "builds", buildMetrics.getBuildMetrics(),
            "deployments", deploymentMetrics.getDeploymentMetrics(),
            "health", healthChecker.getLatestHealth()
        );
    }

    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return userMetrics.getUserMetrics(from, LocalDateTime.now());
    }

    @GetMapping("/builds")
    public Map<String, Object> builds() {
        return buildMetrics.getBuildMetrics();
    }

    @GetMapping("/deployments")
    public Map<String, Object> deployments() {
        return deploymentMetrics.getDeploymentMetrics();
    }

    @GetMapping("/agent")
    public Map<String, Object> agent(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        return agentMetrics.getAgentMetrics(from, LocalDateTime.now());
    }

    @GetMapping("/health")
    public Object health() {
        return healthChecker.getLatestHealth();
    }
}
