package com.cfforge.common.entity;

import com.cfforge.common.enums.DeployEnvironment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.enums.DeployStrategy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "deployments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_id")
    private Build build;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeployStrategy strategy = DeployStrategy.ROLLING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifest_used", columnDefinition = "jsonb")
    private Map<String, Object> manifestUsed;

    @Column(name = "cf_app_guid")
    private String cfAppGuid;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeployEnvironment environment = DeployEnvironment.STAGING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeployStatus status = DeployStatus.PENDING;

    @Column(name = "deployment_url")
    private String deploymentUrl;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
