package com.cfforge.admin.controller;

import com.cfforge.common.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserActivityRepository userActivityRepo;
    private final MetricSnapshotRepository metricSnapshotRepo;
    private final AuditLogRepository auditLogRepo;
    private final ComponentHealthHistoryRepository healthRepo;

    public AdminDashboardController(UserActivityRepository userActivityRepo,
                                     MetricSnapshotRepository metricSnapshotRepo,
                                     AuditLogRepository auditLogRepo,
                                     ComponentHealthHistoryRepository healthRepo) {
        this.userActivityRepo = userActivityRepo;
        this.metricSnapshotRepo = metricSnapshotRepo;
        this.auditLogRepo = auditLogRepo;
        this.healthRepo = healthRepo;
    }

    @GetMapping({"", "/"})
    public String overview(Model model) {
        Instant now = Instant.now();
        Instant dayAgo = now.minus(24, ChronoUnit.HOURS);
        model.addAttribute("activeUsers", userActivityRepo.countDistinctActiveUsers(dayAgo, now));
        model.addAttribute("auditCount", auditLogRepo.countByCreatedAtBetween(dayAgo, now));
        model.addAttribute("healthStatus", healthRepo.findLatestHealthForAllComponents());
        return "admin/overview";
    }

    @GetMapping("/users")
    public String users(Model model) { return "admin/users"; }

    @GetMapping("/agent")
    public String agent(Model model) { return "admin/agent"; }

    @GetMapping("/deployments")
    public String deployments(Model model) { return "admin/deployments"; }

    @GetMapping("/builds")
    public String builds(Model model) { return "admin/builds"; }

    @GetMapping("/health")
    public String health(Model model) { return "admin/health"; }

    @GetMapping("/audit")
    public String audit(Model model) { return "admin/audit"; }

    @GetMapping("/genai")
    public String genai(Model model) { return "admin/genai"; }
}
