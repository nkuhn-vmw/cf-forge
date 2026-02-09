package com.cfforge.api.service;

import com.cfforge.common.entity.Template;
import com.cfforge.common.entity.User;
import com.cfforge.common.enums.Language;
import com.cfforge.common.repository.TemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            Language.valueOf(language.toUpperCase()));
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

    // --- Community marketplace methods ---

    public Page<Template> searchCommunity(String query, String category, String language,
                                           String sortBy, int page, int size) {
        Sort sort = switch (sortBy != null ? sortBy : "popular") {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingSum");
            default -> Sort.by(Sort.Direction.DESC, "downloadCount");
        };
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), sort);

        if (query != null && !query.isBlank()) {
            return templateRepository.searchByNameOrDescription(query, pageable);
        }
        if (category != null && !category.isBlank()) {
            return templateRepository.findByCategoryAndCommunityTrue(category, pageable);
        }
        if (language != null && !language.isBlank()) {
            return templateRepository.findByLanguageAndCommunityTrue(
                Language.valueOf(language.toUpperCase()), pageable);
        }
        return templateRepository.findByCommunityTrue(pageable);
    }

    public Template submitCommunityTemplate(Template template, User author) {
        template.setAuthor(author);
        template.setCommunity(true);
        template.setVerified(false);
        return templateRepository.save(template);
    }

    public Template rateTemplate(String slug, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        Template template = templateRepository.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + slug));
        template.setRatingSum(template.getRatingSum() + rating);
        template.setRatingCount(template.getRatingCount() + 1);
        return templateRepository.save(template);
    }

    public List<Template> getFeatured() {
        return templateRepository.findTop10ByVerifiedTrueOrderByDownloadCountDesc();
    }
}
