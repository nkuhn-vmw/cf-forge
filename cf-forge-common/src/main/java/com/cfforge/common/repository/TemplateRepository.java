package com.cfforge.common.repository;

import com.cfforge.common.entity.Template;
import com.cfforge.common.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findBySlug(String slug);
    List<Template> findByLanguageOrderByDownloadCountDesc(Language language);
}
