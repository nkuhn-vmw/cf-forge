package com.cfforge.admin.controller;

import com.cfforge.admin.service.*;
import com.cfforge.common.entity.MetricSnapshot;
import com.cfforge.common.enums.MetricGranularity;
import com.cfforge.common.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock private UserActivityRepository userActivityRepo;
    @Mock private MetricSnapshotRepository metricSnapshotRepo;
    @Mock private AuditLogRepository auditLogRepo;
    @Mock private ComponentHealthHistoryRepository healthRepo;
    @Mock private UserMetricsService userMetricsService;
    @Mock private BuildMetricsService buildMetricsService;
    @Mock private DeploymentMetricsService deploymentMetricsService;
    @Mock private AgentMetricsService agentMetricsService;
    @Mock private HealthCheckScheduler healthCheckScheduler;

    @BeforeEach
    void setUp() {
        var controller = new AdminDashboardController(
            userActivityRepo, metricSnapshotRepo, auditLogRepo, healthRepo,
            userMetricsService, buildMetricsService, deploymentMetricsService,
            agentMetricsService, healthCheckScheduler);

        // Required to avoid circular view path errors in standalone mode
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();
    }

    @Test
    void overview_returnsOverviewPage_withModelAttributes() throws Exception {
        when(userActivityRepo.countDistinctActiveUsers(any(Instant.class), any(Instant.class)))
            .thenReturn(5L);
        when(auditLogRepo.countByCreatedAtBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(42L);
        when(healthRepo.findLatestHealthForAllComponents())
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/overview"))
            .andExpect(model().attribute("currentPage", "overview"))
            .andExpect(model().attribute("activeUsers", 5L))
            .andExpect(model().attribute("auditCount", 42L))
            .andExpect(model().attributeExists("healthStatus"));
    }

    @Test
    void overview_rootPath_works() throws Exception {
        when(userActivityRepo.countDistinctActiveUsers(any(Instant.class), any(Instant.class)))
            .thenReturn(0L);
        when(auditLogRepo.countByCreatedAtBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(0L);
        when(healthRepo.findLatestHealthForAllComponents())
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/overview"));
    }

    @Test
    void users_returnsUsersPage_withModelAttributes() throws Exception {
        when(userMetricsService.getTotalUsers()).thenReturn(100L);
        when(userMetricsService.getActiveUsers(any())).thenReturn(25L);
        when(userMetricsService.getTopActiveUsers(any(), any(int.class)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/users"))
            .andExpect(model().attribute("currentPage", "users"))
            .andExpect(model().attribute("totalUsers", 100L))
            .andExpect(model().attribute("activeUsers", 25L))
            .andExpect(model().attributeExists("topUsers"));
    }

    @Test
    void agent_returnsAgentPage_withMetrics() throws Exception {
        Map<String, Object> metrics = Map.of(
            "totalPrompts", 50L,
            "refineRequests", 10L,
            "avgResponseTimeMs", 200L,
            "totalRequests", 60L
        );
        when(agentMetricsService.getAgentMetrics(any(), any())).thenReturn(metrics);

        mockMvc.perform(get("/admin/agent"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/agent"))
            .andExpect(model().attribute("currentPage", "agent"))
            .andExpect(model().attribute("metrics", metrics));
    }

    @Test
    void deployments_returnsDeploymentsPage_withMetrics() throws Exception {
        Map<String, Object> metrics = Map.of(
            "totalDeploys", 20L,
            "successfulDeploys", 18L,
            "failedDeploys", 2L,
            "rollbacks", 0L,
            "successRate", 90.0,
            "byStrategy", Map.of(),
            "byEnvironment", Map.of()
        );
        when(deploymentMetricsService.getDeploymentMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/admin/deployments"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/deployments"))
            .andExpect(model().attribute("currentPage", "deployments"))
            .andExpect(model().attribute("metrics", metrics));
    }

    @Test
    void builds_returnsBuildsPage_withMetrics() throws Exception {
        Map<String, Object> metrics = Map.of(
            "totalBuilds", 30L,
            "successfulBuilds", 25L,
            "failedBuilds", 5L,
            "successRate", 83.3,
            "avgDurationMs", 5000L,
            "byStatus", Map.of("SUCCESS", 25L, "FAILED", 5L)
        );
        when(buildMetricsService.getBuildMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/admin/builds"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/builds"))
            .andExpect(model().attribute("currentPage", "builds"))
            .andExpect(model().attribute("metrics", metrics));
    }

    @Test
    void health_returnsHealthPage_withData() throws Exception {
        when(healthCheckScheduler.getLatestHealth()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/health"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/health"))
            .andExpect(model().attribute("currentPage", "health"))
            .andExpect(model().attributeExists("healthData"));
    }

    @Test
    void audit_returnsAuditPage() throws Exception {
        mockMvc.perform(get("/admin/audit"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/audit"))
            .andExpect(model().attribute("currentPage", "audit"));
    }

    @Test
    void genai_returnsGenaiPage_withEmptySnapshots() throws Exception {
        when(metricSnapshotRepo.findByMetricNameAndGranularityAndPeriodStartBetween(
            eq("genai.request"), eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/genai"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/genai"))
            .andExpect(model().attribute("currentPage", "genai"))
            .andExpect(model().attributeExists("metrics"));
    }

    @Test
    void genai_calculatesMetricsCorrectly_withSnapshots() throws Exception {
        var snapshot1 = new MetricSnapshot();
        snapshot1.setCount(10);
        snapshot1.setAvgDurationMs(100.0);
        snapshot1.setP95DurationMs(200.0);

        var snapshot2 = new MetricSnapshot();
        snapshot2.setCount(20);
        snapshot2.setAvgDurationMs(150.0);
        snapshot2.setP95DurationMs(300.0);

        when(metricSnapshotRepo.findByMetricNameAndGranularityAndPeriodStartBetween(
            eq("genai.request"), eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(List.of(snapshot1, snapshot2));

        mockMvc.perform(get("/admin/genai"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("metrics",
                hasEntry("requestVolume", (Object) 30L)))
            .andExpect(model().attribute("metrics",
                hasEntry("avgLatencyMs", (Object) 125.0)))
            .andExpect(model().attribute("metrics",
                hasEntry("p95LatencyMs", (Object) 300.0)));
    }
}
