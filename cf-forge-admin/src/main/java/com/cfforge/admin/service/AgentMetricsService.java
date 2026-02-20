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
        var aggregated = metricSnapshotRepository.aggregateByMetric(MetricGranularity.HOURLY, start, end);

        long totalPrompts = 0;
        double avgDuration = 0;
        long refineCount = 0;

        for (Object[] row : aggregated) {
            String metricName = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double avg = ((Number) row[2]).doubleValue();

            if ("agent.generate".equals(metricName)) {
                totalPrompts = count;
                avgDuration = avg;
            } else if ("agent.refine".equals(metricName)) {
                refineCount = count;
            }
        }

        return Map.of(
            "totalPrompts", totalPrompts,
            "refineRequests", refineCount,
            "avgResponseTimeMs", Math.round(avgDuration),
            "totalRequests", totalPrompts + refineCount
        );
    }
}
