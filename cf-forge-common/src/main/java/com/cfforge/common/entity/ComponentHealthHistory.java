package com.cfforge.common.entity;

import com.cfforge.common.enums.HealthStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "component_health_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentHealthHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthStatus status;

    @Column(name = "cpu_percent")
    private Double cpuPercent;

    @Column(name = "memory_used_mb")
    private Long memoryUsedMb;

    @Column(name = "memory_total_mb")
    private Long memoryTotalMb;

    @Column(name = "instances_running")
    private Integer instancesRunning;

    @Column(name = "instances_desired")
    private Integer instancesDesired;

    @Column(name = "response_time_ms")
    private Double responseTimeMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "recorded_at")
    @Builder.Default
    private Instant recordedAt = Instant.now();
}
