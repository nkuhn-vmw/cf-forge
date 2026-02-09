package com.cfforge.api.controller;

import com.cfforge.api.service.TemplateService;
import com.cfforge.common.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<Template> listTemplates(@RequestParam(required = false) String language) {
        if (language != null && !language.isBlank()) {
            return templateService.listByLanguage(language);
        }
        return templateService.listAll();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Template> getTemplate(@PathVariable String slug) {
        return templateService.getBySlug(slug)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/use")
    public Template useTemplate(@PathVariable String slug) {
        return templateService.useTemplate(slug);
    }

    // --- Community marketplace endpoints ---

    @GetMapping("/community")
    public Page<Template> browseCommunity(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return templateService.searchCommunity(q, category, language, sort, page, size);
    }

    @GetMapping("/featured")
    public List<Template> featured() {
        return templateService.getFeatured();
    }

    @PostMapping("/community/submit")
    public Template submitTemplate(@RequestBody Template template) {
        return templateService.submitCommunityTemplate(template, null);
    }

    @PostMapping("/{slug}/rate")
    public Template rateTemplate(@PathVariable String slug,
                                  @RequestBody Map<String, Integer> body) {
        return templateService.rateTemplate(slug, body.getOrDefault("rating", 5));
    }
}
