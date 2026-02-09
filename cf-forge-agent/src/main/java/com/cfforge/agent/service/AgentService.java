package com.cfforge.agent.service;

import com.cfforge.common.entity.Project;
import com.cfforge.common.events.MetricEventPublisher;
import com.cfforge.common.repository.ProjectRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ProjectRepository projectRepository;
    private final MetricEventPublisher metricPublisher;

    public AgentService(ChatClient chatClient, ProjectRepository projectRepository,
                        MetricEventPublisher metricPublisher) {
        this.chatClient = chatClient;
        this.projectRepository = projectRepository;
        this.metricPublisher = metricPublisher;
    }

    public Flux<String> generate(UUID conversationId, UUID projectId, String userMessage) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String projectContext = buildProjectContext(project);

        return chatClient.prompt()
            .system(s -> s.param("projectContext", projectContext)
                          .param("availableBuildpacks", "java_buildpack_offline, python_buildpack, nodejs_buildpack, go_buildpack, staticfile_buildpack")
                          .param("availableServices", "PostgreSQL, Redis, RabbitMQ, GenAI, S3")
                          .param("currentManifest", project.getCfManifest() != null ? project.getCfManifest().toString() : "none"))
            .user(userMessage)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
            .stream()
            .content();
    }

    public <T> T generateStructured(UUID projectId, String userMessage, Class<T> responseType) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String projectContext = buildProjectContext(project);

        return chatClient.prompt()
            .system(s -> s.param("projectContext", projectContext))
            .user(userMessage)
            .call()
            .entity(responseType);
    }

    private String buildProjectContext(Project project) {
        return String.format("Project: %s, Language: %s, Framework: %s, Status: %s",
            project.getName(), project.getLanguage(), project.getFramework(), project.getStatus());
    }
}
