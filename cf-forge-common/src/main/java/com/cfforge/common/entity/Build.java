package com.cfforge.common.entity;

import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.enums.TriggerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "builds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BuildStatus status = BuildStatus.QUEUED;

    @Column(name = "build_log", columnDefinition = "TEXT")
    private String buildLog;

    @Column(name = "artifact_path")
    private String artifactPath;

    @Column(name = "sbom_path")
    private String sbomPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cve_report", columnDefinition = "jsonb")
    private Map<String, Object> cveReport;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
