package com.cfforge.admin.controller;

import com.cfforge.admin.service.*;
import com.cfforge.common.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserActivityRepository userActivityRepo;
    private final MetricSnapshotRepository metricSnapshotRepo;
    private final AuditLogRepository auditLogRepo;
    private final ComponentHealthHistoryRepository healthRepo;
    private final UserMetricsService userMetricsService;
    private final BuildMetricsService buildMetricsService;
    private final DeploymentMetricsService deploymentMetricsService;
    private final AgentMetricsService agentMetricsService;
    private final HealthCheckScheduler healthCheckScheduler;

    public AdminDashboardController(UserActivityRepository userActivityRepo,
                                     MetricSnapshotRepository metricSnapshotRepo,
                                     AuditLogRepository auditLogRepo,
                                     ComponentHealthHistoryRepository healthRepo,
                                     UserMetricsService userMetricsService,
                                     BuildMetricsService buildMetricsService,
                                     DeploymentMetricsService deploymentMetricsService,
                                     AgentMetricsService agentMetricsService,
                                     HealthCheckScheduler healthCheckScheduler) {
        this.userActivityRepo = userActivityRepo;
        this.metricSnapshotRepo = metricSnapshotRepo;
        this.auditLogRepo = auditLogRepo;
        this.healthRepo = healthRepo;
        this.userMetricsService = userMetricsService;
        this.buildMetricsService = buildMetricsService;
        this.deploymentMetricsService = deploymentMetricsService;
        this.agentMetricsService = agentMetricsService;
        this.healthCheckScheduler = healthCheckScheduler;
    }

    @ModelAttribute("currentPage")
    public String currentPage() {
        return "";
    }

    @GetMapping({"", "/"})
    public String overview(Model model) {
        model.addAttribute("currentPage", "overview");
        Instant now = Instant.now();
        Instant dayAgo = now.minus(24, ChronoUnit.HOURS);
        model.addAttribute("activeUsers", userActivityRepo.countDistinctActiveUsers(dayAgo, now));
        model.addAttribute("auditCount", auditLogRepo.countByCreatedAtBetween(dayAgo, now));
        model.addAttribute("healthStatus", healthRepo.findLatestHealthForAllComponents());
        return "admin/overview";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("currentPage", "users");
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        model.addAttribute("totalUsers", userMetricsService.getTotalUsers());
        model.addAttribute("activeUsers", userMetricsService.getActiveUsers(since));
        model.addAttribute("topUsers", userMetricsService.getTopActiveUsers(since, 10));
        return "admin/users";
    }

    @GetMapping("/agent")
    public String agent(Model model) {
        model.addAttribute("currentPage", "agent");
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        var metrics = agentMetricsService.getAgentMetrics(from, LocalDateTime.now());
        model.addAttribute("metrics", metrics);
        return "admin/agent";
    }

    @GetMapping("/deployments")
    public String deployments(Model model) {
        model.addAttribute("currentPage", "deployments");
        var metrics = deploymentMetricsService.getDeploymentMetrics();
        model.addAttribute("metrics", metrics);
        return "admin/deployments";
    }

    @GetMapping("/builds")
    public String builds(Model model) {
        model.addAttribute("currentPage", "builds");
        var metrics = buildMetricsService.getBuildMetrics();
        model.addAttribute("metrics", metrics);
        return "admin/builds";
    }

    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("currentPage", "health");
        model.addAttribute("healthData", healthCheckScheduler.getLatestHealth());
        return "admin/health";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("currentPage", "audit");
        return "admin/audit";
    }

    @GetMapping("/genai")
    public String genai(Model model) {
        model.addAttribute("currentPage", "genai");
        Instant start = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant end = Instant.now();
        var snapshots = metricSnapshotRepo.findByMetricNameAndGranularityAndPeriodStartBetween(
            "genai.request", com.cfforge.common.enums.MetricGranularity.HOURLY, start, end);

        long totalRequests = snapshots.stream().mapToLong(s -> s.getCount()).sum();
        double avgLatency = snapshots.stream()
            .mapToDouble(s -> s.getAvgDurationMs())
            .average().orElse(0);
        double p95Latency = snapshots.stream()
            .mapToDouble(s -> s.getAvgDurationMs())
            .sorted()
            .skip((long)(snapshots.size() * 0.95))
            .findFirst().orElse(0);

        var metrics = new java.util.LinkedHashMap<String, Object>();
        metrics.put("requestVolume", totalRequests);
        metrics.put("avgLatencyMs", Math.round(avgLatency * 100.0) / 100.0);
        metrics.put("p95LatencyMs", Math.round(p95Latency * 100.0) / 100.0);
        model.addAttribute("metrics", metrics);
        return "admin/genai";
    }
}
