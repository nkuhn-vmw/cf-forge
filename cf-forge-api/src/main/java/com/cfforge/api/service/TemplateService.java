package com.cfforge.api.service;

import com.cfforge.common.entity.Template;
import com.cfforge.common.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<Template> listAll() {
        return templateRepository.findAll();
    }

    public List<Template> listByLanguage(String language) {
        return templateRepository.findByLanguageOrderByDownloadCountDesc(
            com.cfforge.common.enums.Language.valueOf(language.toUpperCase()));
    }

    public Optional<Template> getBySlug(String slug) {
        return templateRepository.findBySlug(slug);
    }

    public Template useTemplate(String slug) {
        Template template = templateRepository.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + slug));
        template.setDownloadCount(template.getDownloadCount() + 1);
        return templateRepository.save(template);
    }
}
