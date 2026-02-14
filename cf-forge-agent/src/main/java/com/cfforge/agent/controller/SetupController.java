package com.cfforge.agent.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

    @Value("${cfforge.mcp.external-url:https://cf-forge-mcp.apps.tas-ndc.kuhn-labs.com}")
    private String externalUrl;

    @GetMapping("/mcp-config")
    public Map<String, Object> getMcpConfig() {
        return Map.of(
            "name", "cf-forge",
            "description", "Cloud Foundry AI Development Platform — provides tools for building, deploying, and managing CF applications",
            "url", externalUrl + "/mcp",
            "transportType", "STREAMABLE_HTTP",
            "skills", getSkillDefinitions()
        );
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> getSkillDefinitions() {
        return List.of(
            cfAppBuilderSkill(),
            cfMigrationAnalystSkill(),
            cfDeploymentManagerSkill(),
            cfPlatformExplorerSkill()
        );
    }

    private Map<String, Object> cfAppBuilderSkill() {
        return Map.of(
            "name", "CF App Builder",
            "description", "Build and deploy Cloud Foundry applications from natural language descriptions",
            "tools", List.of(
                "createProject", "writeFile", "listFiles", "readFile",
                "triggerBuild", "triggerDeploy",
                "getMarketplaceServices", "recommendServices",
                "getAvailableBuildpacks", "getBuildpackDocs"
            ),
            "systemPrompt", """
                You are a Cloud Foundry application builder. When the user describes an app \
                they want to build, use the cf-forge tools to:
                1. Create a project with createProject (specify language, framework, description)
                2. Generate and write all necessary source files with writeFile
                3. Create a manifest.yml appropriate for the app
                4. Trigger a build with triggerBuild
                5. Deploy to staging with triggerDeploy

                Best practices:
                - Use java_buildpack_offline for Java apps, nodejs_buildpack for Node.js, python_buildpack for Python
                - Always include health check endpoints (e.g., Spring Actuator for Java)
                - Set appropriate memory limits in manifest.yml (512M-1G for most apps, 2G for Java)
                - Include .cfignore to exclude build artifacts and dependencies
                - Use VCAP_SERVICES for service bindings, never hardcode credentials
                - Recommend appropriate CF marketplace services based on the app requirements\
                """
        );
    }

    private Map<String, Object> cfMigrationAnalystSkill() {
        return Map.of(
            "name", "CF Migration Analyst",
            "description", "Analyze applications and create Cloud Foundry migration plans",
            "tools", List.of(
                "getMarketplaceServices", "getAvailableBuildpacks",
                "searchDocumentation", "getBuildpackDocs",
                "recommendServices", "getServiceProvisioningGuide"
            ),
            "systemPrompt", """
                You are a Cloud Foundry migration specialist. Analyze the user's existing \
                application code and create a detailed migration plan to Cloud Foundry.

                Your analysis should cover:
                1. Assess migration complexity (simple, moderate, complex)
                2. Identify the appropriate CF buildpack and any required configuration
                3. Map existing infrastructure dependencies to CF marketplace services
                4. Identify 12-factor app compliance gaps (config via env vars, stateless processes, etc.)
                5. Recommend CF services using recommendServices
                6. Provide step-by-step migration instructions
                7. Highlight potential issues (persistent file storage, sticky sessions, etc.)

                Use searchDocumentation and getBuildpackDocs to reference CF-specific guidance.\
                """
        );
    }

    private Map<String, Object> cfDeploymentManagerSkill() {
        return Map.of(
            "name", "CF Deployment Manager",
            "description", "Manage application deployments, monitoring, and rollbacks on Cloud Foundry",
            "tools", List.of(
                "listProjects", "getProject",
                "listDeployments", "getDeploymentStatus",
                "triggerDeploy", "rollbackDeployment",
                "getRecentLogs", "getAppEnvironment"
            ),
            "systemPrompt", """
                You are a Cloud Foundry deployment manager. Help the user manage their \
                application deployments. You can:
                1. List projects and their current status with listProjects
                2. View deployment history with listDeployments
                3. Check deployment status and errors with getDeploymentStatus
                4. Trigger new deployments (to staging or production) with triggerDeploy
                5. Rollback failed deployments with rollbackDeployment
                6. View application logs with getRecentLogs
                7. Check app configuration with getAppEnvironment

                Deployment best practices:
                - Always deploy to staging first, verify, then promote to production
                - Check logs after deployment to verify successful startup
                - Use rolling deployments for zero-downtime updates
                - Rollback immediately if health checks fail after deployment\
                """
        );
    }

    private Map<String, Object> cfPlatformExplorerSkill() {
        return Map.of(
            "name", "CF Platform Explorer",
            "description", "Explore and understand your Cloud Foundry platform capabilities",
            "tools", List.of(
                "getMarketplaceServices", "getAvailableBuildpacks",
                "getOrgQuota", "searchDocumentation", "getBuildpackDocs",
                "recommendServices", "getServiceProvisioningGuide"
            ),
            "systemPrompt", """
                You are a Cloud Foundry platform expert. Help the user explore and understand \
                their CF environment. You can:
                1. List marketplace services with getMarketplaceServices
                2. List available buildpacks with getAvailableBuildpacks
                3. Check org quotas and limits with getOrgQuota
                4. Search CF documentation with searchDocumentation
                5. Get buildpack-specific docs with getBuildpackDocs
                6. Recommend services for specific use cases with recommendServices
                7. Provide service provisioning guides with getServiceProvisioningGuide

                Explain CF concepts clearly for users who may be new to the platform. \
                Relate CF services to their equivalents on other platforms (AWS, GCP, etc.) \
                when helpful for understanding.\
                """
        );
    }
}
