package com.cfforge.common.repository;

import com.cfforge.common.entity.MetricSnapshot;
import com.cfforge.common.enums.MetricGranularity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, UUID> {

    List<MetricSnapshot> findByMetricNameAndGranularityAndPeriodStartBetween(
        String metricName, MetricGranularity granularity, Instant start, Instant end);

    List<MetricSnapshot> findByGranularityAndPeriodStartBetween(
        MetricGranularity granularity, Instant start, Instant end);

    @Query("SELECT m.metricName, SUM(m.count), AVG(m.avgDurationMs) FROM MetricSnapshot m " +
           "WHERE m.granularity = :gran AND m.periodStart BETWEEN :start AND :end " +
           "GROUP BY m.metricName")
    List<Object[]> aggregateByMetric(MetricGranularity gran, Instant start, Instant end);
}
