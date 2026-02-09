package com.cfforge.common.entity;

import com.cfforge.common.enums.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    private String description;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    private String framework;

    private String buildpack;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifest_template", columnDefinition = "jsonb")
    private Map<String, Object> manifestTemplate;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    // Community marketplace fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "is_community")
    @Builder.Default
    private Boolean community = false;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "rating_sum")
    @Builder.Default
    private Integer ratingSum = 0;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
