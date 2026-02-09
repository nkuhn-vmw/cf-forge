package com.cfforge.api.controller;

import com.cfforge.api.service.TemplateService;
import com.cfforge.common.entity.Template;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
