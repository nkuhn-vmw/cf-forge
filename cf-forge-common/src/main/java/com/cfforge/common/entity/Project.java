package com.cfforge.common.entity;

import com.cfforge.common.enums.Language;
import com.cfforge.common.enums.ProjectStatus;
import com.cfforge.common.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    private String framework;

    private String buildpack;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cf_manifest", columnDefinition = "jsonb")
    private Map<String, Object> cfManifest;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cf_target_id")
    private CfTarget cfTarget;

    @Column(name = "cf_app_guid")
    private String cfAppGuid;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
