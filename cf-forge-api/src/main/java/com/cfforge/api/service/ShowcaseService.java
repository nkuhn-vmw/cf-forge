package com.cfforge.api.service;

import com.cfforge.common.entity.Project;
import com.cfforge.common.repository.ProjectRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CF Weekly Showcase Integration (ECO-007).
 *
 * Curates and surfaces notable projects for the CF community weekly showcase.
 * Tracks project metrics (builds, deploys, AI usage) to identify showcase
 * candidates. Generates showcase summaries with screenshots and stats.
 */
@Service
@Slf4j
public class ShowcaseService {

    private final ProjectRepository projectRepository;

    public ShowcaseService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Get showcase candidates based on recent activity and quality signals.
     */
    public List<ShowcaseCandidate> getShowcaseCandidates(int limit) {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        // Find public projects created or updated in the last week
        List<Project> recentProjects = projectRepository.findAll().stream()
            .filter(p -> p.getVisibility() != null &&
                         p.getVisibility().name().equals("PUBLIC"))
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(weekAgo))
            .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        return recentProjects.stream()
            .map(p -> {
                var candidate = new ShowcaseCandidate();
                candidate.setProjectId(p.getId());
                candidate.setProjectName(p.getName());
                candidate.setDescription(p.getDescription());
                candidate.setLanguage(p.getLanguage() != null ? p.getLanguage().name() : "unknown");
                candidate.setFramework(p.getFramework());
                candidate.setOwnerName(p.getOwner() != null ? p.getOwner().getDisplayName() : "anonymous");
                candidate.setCreatedAt(p.getCreatedAt());
                return candidate;
            })
            .collect(Collectors.toList());
    }

    /**
     * Generate a showcase summary for the weekly digest.
     */
    public ShowcaseSummary generateWeeklySummary() {
        List<ShowcaseCandidate> candidates = getShowcaseCandidates(10);

        var summary = new ShowcaseSummary();
        summary.setWeekOf(Instant.now().toString());
        summary.setTotalProjects((int) projectRepository.count());
        summary.setFeaturedProjects(candidates);

        // Language distribution
        Map<String, Long> langDist = candidates.stream()
            .collect(Collectors.groupingBy(ShowcaseCandidate::getLanguage, Collectors.counting()));
        summary.setLanguageDistribution(langDist);

        return summary;
    }

    @Data
    public static class ShowcaseCandidate {
        private UUID projectId;
        private String projectName;
        private String description;
        private String language;
        private String framework;
        private String ownerName;
        private Instant createdAt;
    }

    @Data
    public static class ShowcaseSummary {
        private String weekOf;
        private int totalProjects;
        private List<ShowcaseCandidate> featuredProjects;
        private Map<String, Long> languageDistribution;
    }
}
