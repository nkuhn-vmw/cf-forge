package com.cfforge.agent.service;

import com.cfforge.agent.model.ErrorFix;
import com.cfforge.common.entity.Build;
import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.repository.BuildRepository;
import com.cfforge.common.repository.DeploymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.util.ResourceUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ErrorAnalysisService {

    private final ChatClient chatClient;
    private final BuildRepository buildRepository;
    private final DeploymentRepository deploymentRepository;

    public ErrorAnalysisService(ChatClient chatClient, BuildRepository buildRepository,
                                 DeploymentRepository deploymentRepository) {
        this.chatClient = chatClient;
        this.buildRepository = buildRepository;
        this.deploymentRepository = deploymentRepository;
    }

    public ErrorFix analyzeBuildFailure(UUID projectId) {
        Build build = buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .filter(b -> b.getStatus() == BuildStatus.FAILED)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No failed builds found for project"));

        return chatClient.prompt()
            .system(ResourceUtils.getText("classpath:prompts/debugging.st"))
            .user("Analyze this build failure and suggest fixes:\n\n" +
                  "Build Log:\n" + truncateLog(build.getBuildLog()))
            .call()
            .entity(ErrorFix.class);
    }

    public ErrorFix analyzeDeployFailure(UUID projectId) {
        Deployment deployment = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .filter(d -> d.getStatus() == DeployStatus.FAILED)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No failed deployments found for project"));

        return chatClient.prompt()
            .system(ResourceUtils.getText("classpath:prompts/debugging.st"))
            .user("Analyze this CF deployment failure and suggest fixes:\n\n" +
                  "Deployment details: strategy=" + deployment.getStrategy() +
                  ", environment=" + deployment.getEnvironment())
            .call()
            .entity(ErrorFix.class);
    }

    private String truncateLog(String log) {
        if (log == null) return "No log available";
        return log.length() > 8000 ? log.substring(log.length() - 8000) : log;
    }
}
