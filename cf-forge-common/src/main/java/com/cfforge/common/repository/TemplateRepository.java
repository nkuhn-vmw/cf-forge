package com.cfforge.common.repository;

import com.cfforge.common.entity.Template;
import com.cfforge.common.enums.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findBySlug(String slug);
    List<Template> findByLanguageOrderByDownloadCountDesc(Language language);

    // Community marketplace queries
    Page<Template> findByCommunityTrue(Pageable pageable);
    Page<Template> findByCategoryAndCommunityTrue(String category, Pageable pageable);
    Page<Template> findByLanguageAndCommunityTrue(Language language, Pageable pageable);
    List<Template> findTop10ByVerifiedTrueOrderByDownloadCountDesc();

    @Query("SELECT t FROM Template t WHERE t.community = true AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Template> searchByNameOrDescription(@Param("query") String query, Pageable pageable);
}
