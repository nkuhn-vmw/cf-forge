package com.cfforge.admin.controller;

import com.cfforge.admin.service.*;
import com.cfforge.common.enums.MetricGranularity;
import com.cfforge.common.repository.ComponentHealthHistoryRepository;
import com.cfforge.common.repository.MetricSnapshotRepository;
import com.cfforge.common.repository.UserActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private final UserMetricsService userMetrics;
    private final BuildMetricsService buildMetrics;
    private final DeploymentMetricsService deploymentMetrics;
    private final AgentMetricsService agentMetrics;
    private final HealthCheckScheduler healthChecker;
    private final ComponentHealthHistoryRepository healthHistoryRepo;
    private final MetricSnapshotRepository metricSnapshotRepo;
    private final UserActivityRepository userActivityRepo;

    public AdminApiController(UserMetricsService userMetrics, BuildMetricsService buildMetrics,
                               DeploymentMetricsService deploymentMetrics, AgentMetricsService agentMetrics,
                               HealthCheckScheduler healthChecker,
                               ComponentHealthHistoryRepository healthHistoryRepo,
                               MetricSnapshotRepository metricSnapshotRepo,
                               UserActivityRepository userActivityRepo) {
        this.userMetrics = userMetrics;
        this.buildMetrics = buildMetrics;
        this.deploymentMetrics = deploymentMetrics;
        this.agentMetrics = agentMetrics;
        this.healthChecker = healthChecker;
        this.healthHistoryRepo = healthHistoryRepo;
        this.metricSnapshotRepo = metricSnapshotRepo;
        this.userActivityRepo = userActivityRepo;
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

    @GetMapping("/users/{userId}/activity")
    public Object userActivity(@PathVariable UUID userId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        return userActivityRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
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

    @GetMapping("/genai")
    public Map<String, Object> genai(
            @RequestParam(defaultValue = "7") int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant end = Instant.now();
        var snapshots = metricSnapshotRepo.findByMetricNameAndGranularityAndPeriodStartBetween(
            "genai.request", MetricGranularity.HOURLY, start, end);

        long totalRequests = snapshots.stream().mapToLong(s -> s.getCount()).sum();
        double avgLatency = snapshots.stream()
            .mapToDouble(s -> s.getAvgDurationMs())
            .average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", totalRequests);
        result.put("avgLatencyMs", Math.round(avgLatency * 100.0) / 100.0);
        result.put("periodDays", days);
        result.put("timeseries", snapshots);
        return result;
    }

    @GetMapping("/health")
    public Object health() {
        return healthChecker.getLatestHealth();
    }

    @GetMapping("/health/history")
    public Object healthHistory(
            @RequestParam(required = false) String component,
            @RequestParam(defaultValue = "1") int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant end = Instant.now();
        if (component != null && !component.isBlank()) {
            return healthHistoryRepo.findByComponentNameAndRecordedAtBetweenOrderByRecordedAtDesc(
                component, start, end);
        }
        return healthHistoryRepo.findLatestHealthForAllComponents();
    }

    @GetMapping("/metrics/timeseries")
    public Object timeseries(
            @RequestParam String metric,
            @RequestParam(defaultValue = "HOURLY") String granularity,
            @RequestParam(defaultValue = "7") int days) {
        Instant start = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant end = Instant.now();
        MetricGranularity gran = MetricGranularity.valueOf(granularity);
        return metricSnapshotRepo.findByMetricNameAndGranularityAndPeriodStartBetween(
            metric, gran, start, end);
    }
}
