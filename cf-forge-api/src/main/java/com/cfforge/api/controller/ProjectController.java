package com.cfforge.api.controller;

import com.cfforge.common.entity.Project;
import com.cfforge.common.entity.User;
import com.cfforge.common.enums.Language;
import com.cfforge.common.repository.ProjectRepository;
import com.cfforge.common.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Project> listProjects(@AuthenticationPrincipal Jwt jwt) {
        User user = getOrCreateUser(jwt);
        return projectRepository.findByOwnerIdOrderByUpdatedAtDesc(user.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable UUID id) {
        return projectRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody Map<String, String> body) {
        User user = getOrCreateUser(jwt);
        String name = body.get("name");
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");

        var project = Project.builder()
            .owner(user)
            .name(name)
            .slug(slug)
            .description(body.get("description"))
            .language(Language.valueOf(body.getOrDefault("language", "JAVA")))
            .framework(body.get("framework"))
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable UUID id,
                                                   @RequestBody Map<String, String> body) {
        return projectRepository.findById(id)
            .map(project -> {
                if (body.containsKey("name")) project.setName(body.get("name"));
                if (body.containsKey("description")) project.setDescription(body.get("description"));
                if (body.containsKey("framework")) project.setFramework(body.get("framework"));
                if (body.containsKey("buildpack")) project.setBuildpack(body.get("buildpack"));
                if (body.containsKey("language")) {
                    project.setLanguage(Language.valueOf(body.get("language")));
                }
                project.setUpdatedAt(Instant.now());
                return ResponseEntity.ok(projectRepository.save(project));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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
}
