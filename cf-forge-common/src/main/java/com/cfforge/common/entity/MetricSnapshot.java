package com.cfforge.common.entity;

import com.cfforge.common.enums.MetricGranularity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "metric_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricGranularity granularity;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Builder.Default
    private long count = 0;

    @Column(name = "sum_duration_ms")
    @Builder.Default
    private double sumDurationMs = 0;

    @Column(name = "avg_duration_ms")
    @Builder.Default
    private double avgDurationMs = 0;

    @Column(name = "p95_duration_ms")
    @Builder.Default
    private double p95DurationMs = 0;

    @Column(name = "error_count")
    @Builder.Default
    private long errorCount = 0;

    @Column(name = "success_count")
    @Builder.Default
    private long successCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> dimensions;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
