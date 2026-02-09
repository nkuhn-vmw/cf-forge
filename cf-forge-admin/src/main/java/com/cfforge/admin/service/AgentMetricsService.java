package com.cfforge.admin.service;

import com.cfforge.common.enums.MetricGranularity;
import com.cfforge.common.repository.MetricSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class AgentMetricsService {

    private final MetricSnapshotRepository metricSnapshotRepository;

    public AgentMetricsService(MetricSnapshotRepository metricSnapshotRepository) {
        this.metricSnapshotRepository = metricSnapshotRepository;
    }

    public Map<String, Object> getAgentMetrics(LocalDateTime from, LocalDateTime to) {
        Instant start = from.toInstant(ZoneOffset.UTC);
        Instant end = to.toInstant(ZoneOffset.UTC);
        var generateMetrics = metricSnapshotRepository.aggregateByMetric(MetricGranularity.HOURLY, start, end);
        var refineMetrics = metricSnapshotRepository.aggregateByMetric(MetricGranularity.HOURLY, start, end);

        long totalPrompts = 0;
        double avgDuration = 0;

        if (!generateMetrics.isEmpty()) {
            Object[] row = generateMetrics.get(0);
            totalPrompts = ((Number) row[0]).longValue();
            avgDuration = ((Number) row[1]).doubleValue();
        }

        long refineCount = 0;
        if (!refineMetrics.isEmpty()) {
            Object[] row = refineMetrics.get(0);
            refineCount = ((Number) row[0]).longValue();
        }

        return Map.of(
            "totalPrompts", totalPrompts,
            "refineRequests", refineCount,
            "avgResponseTimeMs", Math.round(avgDuration),
            "totalRequests", totalPrompts + refineCount
        );
    }
}
