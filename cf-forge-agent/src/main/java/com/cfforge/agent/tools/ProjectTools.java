package com.cfforge.agent.tools;

import com.cfforge.common.entity.Project;
import com.cfforge.common.entity.User;
import com.cfforge.common.enums.Language;
import com.cfforge.common.enums.ProjectStatus;
import com.cfforge.common.repository.ProjectRepository;
import com.cfforge.common.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProjectTools {

    private static final String MCP_SERVICE_UAA_ID = "mcp-service";
    private static final String MCP_SERVICE_EMAIL = "mcp-service@cf-forge.internal";

    private static final Logger log = LoggerFactory.getLogger(ProjectTools.class);
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WebClient workspaceClient;

    public ProjectTools(ProjectRepository projectRepository, UserRepository userRepository, WebClient workspaceClient) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.workspaceClient = workspaceClient;
    }

    private User getOrCreateMcpServiceUser() {
        return userRepository.findByUaaUserId(MCP_SERVICE_UAA_ID)
            .orElseGet(() -> userRepository.save(User.builder()
                .uaaUserId(MCP_SERVICE_UAA_ID)
                .email(MCP_SERVICE_EMAIL)
                .displayName("MCP Service Account")
                .build()));
    }

    @Tool(description = "Create a new CF Forge project with a workspace for file storage. " +
                        "Returns the project ID and workspace ID for subsequent file and build operations.")
    public String createProject(
            @ToolParam(description = "Project name (e.g., 'my-spring-api')") String name,
            @ToolParam(description = "Programming language: JAVA, PYTHON, NODEJS, GO, DOTNET, RUBY, or STATICFILE") String language,
            @ToolParam(description = "Framework (e.g., 'spring-boot', 'flask', 'express')") String framework,
            @ToolParam(description = "Short description of the project") String description) {
        try {
            // Create workspace via workspace service
            String workspaceId = workspaceClient.post()
                .uri("/workspace")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            Language lang = Language.valueOf(language.toUpperCase());
            String buildpack = inferBuildpack(lang);

            User owner = getOrCreateMcpServiceUser();

            Project project = Project.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .language(lang)
                .framework(framework)
                .buildpack(buildpack)
                .owner(owner)
                .status(ProjectStatus.ACTIVE)
                .workspaceId(workspaceId != null ? UUID.fromString(workspaceId.replace("\"", "")) : UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            project = projectRepository.save(project);

            return String.format("Project created successfully!\n" +
                "Project ID: %s\nWorkspace ID: %s\nName: %s\nLanguage: %s\nFramework: %s\nBuildpack: %s",
                project.getId(), project.getWorkspaceId(), project.getName(),
                project.getLanguage(), project.getFramework(), project.getBuildpack());
        } catch (Exception e) {
            log.error("Failed to create project: {}", e.getMessage(), e);
            return "Failed to create project: " + e.getMessage();
        }
    }

    @Tool(description = "List all projects. Returns project names, IDs, status, and language.")
    public String listProjects() {
        try {
            List<Project> projects = projectRepository.findAll();
            if (projects.isEmpty()) {
                return "No projects found.";
            }
            return projects.stream()
                .map(p -> String.format("- %s (ID: %s, Language: %s, Status: %s, Workspace: %s)",
                    p.getName(), p.getId(), p.getLanguage(), p.getStatus(), p.getWorkspaceId()))
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to list projects: {}", e.getMessage(), e);
            return "Failed to list projects: " + e.getMessage();
        }
    }

    @Tool(description = "Get detailed information about a specific project by ID")
    public String getProject(@ToolParam(description = "Project UUID") String projectId) {
        try {
            return projectRepository.findById(UUID.fromString(projectId))
                .map(p -> String.format(
                    "Project: %s\nID: %s\nSlug: %s\nDescription: %s\nLanguage: %s\n" +
                    "Framework: %s\nBuildpack: %s\nStatus: %s\nWorkspace ID: %s\n" +
                    "CF App GUID: %s\nCreated: %s\nUpdated: %s",
                    p.getName(), p.getId(), p.getSlug(), p.getDescription(),
                    p.getLanguage(), p.getFramework(), p.getBuildpack(),
                    p.getStatus(), p.getWorkspaceId(),
                    p.getCfAppGuid() != null ? p.getCfAppGuid() : "not deployed",
                    p.getCreatedAt(), p.getUpdatedAt()))
                .orElse("Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to get project: {}", e.getMessage(), e);
            return "Failed to get project: " + e.getMessage();
        }
    }

    @Tool(description = "Update a project's name, description, framework, or buildpack")
    public String updateProject(
            @ToolParam(description = "Project UUID") String projectId,
            @ToolParam(description = "New project name (or empty to keep current)", required = false) String name,
            @ToolParam(description = "New description (or empty to keep current)", required = false) String description,
            @ToolParam(description = "New framework (or empty to keep current)", required = false) String framework) {
        try {
            return projectRepository.findById(UUID.fromString(projectId))
                .map(p -> {
                    if (name != null && !name.isBlank()) {
                        p.setName(name);
                        p.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
                    }
                    if (description != null && !description.isBlank()) {
                        p.setDescription(description);
                    }
                    if (framework != null && !framework.isBlank()) {
                        p.setFramework(framework);
                    }
                    p.setUpdatedAt(Instant.now());
                    projectRepository.save(p);
                    return "Project updated: " + p.getName() + " (ID: " + p.getId() + ")";
                })
                .orElse("Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to update project: {}", e.getMessage(), e);
            return "Failed to update project: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a project by marking it as DELETED")
    public String deleteProject(@ToolParam(description = "Project UUID") String projectId) {
        try {
            return projectRepository.findById(UUID.fromString(projectId))
                .map(p -> {
                    p.setStatus(ProjectStatus.DELETED);
                    p.setUpdatedAt(Instant.now());
                    projectRepository.save(p);
                    return "Project deleted: " + p.getName() + " (ID: " + p.getId() + ")";
                })
                .orElse("Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to delete project: {}", e.getMessage(), e);
            return "Failed to delete project: " + e.getMessage();
        }
    }

    private String inferBuildpack(Language language) {
        return switch (language) {
            case JAVA -> "java_buildpack_offline";
            case PYTHON -> "python_buildpack";
            case NODEJS -> "nodejs_buildpack";
            case GO -> "go_buildpack";
            case DOTNET -> "dotnet_core_buildpack";
            case RUBY -> "ruby_buildpack";
            case STATICFILE -> "staticfile_buildpack";
        };
    }
}
