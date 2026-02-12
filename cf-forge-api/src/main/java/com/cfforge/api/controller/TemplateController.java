package com.cfforge.api.controller;

import com.cfforge.api.service.TemplateService;
import com.cfforge.common.entity.Project;
import com.cfforge.common.entity.Template;
import com.cfforge.common.entity.User;
import com.cfforge.common.repository.ProjectRepository;
import com.cfforge.common.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TemplateController(TemplateService templateService,
                               ProjectRepository projectRepository,
                               UserRepository userRepository) {
        this.templateService = templateService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
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
    public Project useTemplate(@PathVariable String slug,
                                @AuthenticationPrincipal Jwt jwt) {
        Template template = templateService.useTemplate(slug);
        User user = getOrCreateUser(jwt);

        String projectName = template.getName() + " Project";
        String projectSlug = projectName.toLowerCase().replaceAll("[^a-z0-9]+", "-");

        var project = Project.builder()
            .owner(user)
            .name(projectName)
            .slug(projectSlug)
            .description("Created from template: " + template.getName())
            .language(template.getLanguage())
            .framework(template.getFramework())
            .buildpack(template.getBuildpack())
            .cfManifest(template.getManifestTemplate())
            .build();

        return projectRepository.save(project);
    }

    private User getOrCreateUser(Jwt jwt) {
        String uaaUserId = jwt.getSubject();
        return userRepository.findByUaaUserId(uaaUserId)
            .orElseGet(() -> userRepository.save(User.builder()
                .uaaUserId(uaaUserId)
                .email(jwt.getClaimAsString("email"))
                .displayName(jwt.getClaimAsString("user_name"))
                .build()));
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
