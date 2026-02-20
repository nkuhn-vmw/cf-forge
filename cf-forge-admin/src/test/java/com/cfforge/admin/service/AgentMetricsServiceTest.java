package com.cfforge.admin.service;

import com.cfforge.common.enums.MetricGranularity;
import com.cfforge.common.repository.MetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentMetricsServiceTest {

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;

    private AgentMetricsService service;

    @BeforeEach
    void setUp() {
        service = new AgentMetricsService(metricSnapshotRepository);
    }

    @Test
    void getAgentMetrics_withBothMetricTypes_aggregatesCorrectly() {
        Object[] generateRow = {"agent.generate", 100L, 250.0};
        Object[] refineRow = {"agent.refine", 30L, 180.0};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(generateRow);
        rows.add(refineRow);
        when(metricSnapshotRepository.aggregateByMetric(eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(rows);

        Map<String, Object> result = service.getAgentMetrics(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(result.get("totalPrompts")).isEqualTo(100L);
        assertThat(result.get("refineRequests")).isEqualTo(30L);
        assertThat(result.get("avgResponseTimeMs")).isEqualTo(250L);
        assertThat(result.get("totalRequests")).isEqualTo(130L);
    }

    @Test
    void getAgentMetrics_withOnlyGenerateMetrics_returnsZeroRefine() {
        Object[] generateRow = {"agent.generate", 50L, 300.0};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(generateRow);
        when(metricSnapshotRepository.aggregateByMetric(eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(rows);

        Map<String, Object> result = service.getAgentMetrics(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(result.get("totalPrompts")).isEqualTo(50L);
        assertThat(result.get("refineRequests")).isEqualTo(0L);
        assertThat(result.get("totalRequests")).isEqualTo(50L);
    }

    @Test
    void getAgentMetrics_withNoMetrics_returnsZeros() {
        when(metricSnapshotRepository.aggregateByMetric(eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.getAgentMetrics(
            LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(result.get("totalPrompts")).isEqualTo(0L);
        assertThat(result.get("refineRequests")).isEqualTo(0L);
        assertThat(result.get("avgResponseTimeMs")).isEqualTo(0L);
        assertThat(result.get("totalRequests")).isEqualTo(0L);
    }

    @Test
    void getAgentMetrics_callsRepositoryOnce() {
        when(metricSnapshotRepository.aggregateByMetric(eq(MetricGranularity.HOURLY), any(), any()))
            .thenReturn(Collections.emptyList());

        service.getAgentMetrics(LocalDateTime.now().minusDays(7), LocalDateTime.now());

        verify(metricSnapshotRepository).aggregateByMetric(eq(MetricGranularity.HOURLY), any(), any());
    }
}
