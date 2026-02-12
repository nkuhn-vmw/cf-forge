package com.cfforge.agent.tools;

import com.cfforge.common.enums.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ServiceTools {

    private static final Logger log = LoggerFactory.getLogger(ServiceTools.class);
    private final WebClient cfApiClient;

    public ServiceTools(WebClient cfApiClient) {
        this.cfApiClient = cfApiClient;
    }

    @Tool(description = "Recommend CF marketplace services for a project based on its language, " +
                        "framework, and requirements. Returns service names with justification.")
    public String recommendServices(
            @ToolParam(description = "Programming language (e.g., JAVA, PYTHON, NODEJS)") String language,
            @ToolParam(description = "Framework (e.g., spring-boot, flask, express)") String framework,
            @ToolParam(description = "Comma-separated requirements (e.g., 'database,caching,messaging')") String requirements) {
        StringBuilder sb = new StringBuilder("Recommended CF services for " + language + "/" + framework + ":\n\n");

        Map<String, String> recommendations = new LinkedHashMap<>();

        if (requirements != null) {
            String reqs = requirements.toLowerCase();
            if (reqs.contains("database") || reqs.contains("sql") || reqs.contains("postgres")) {
                recommendations.put("PostgreSQL (cf-forge-db)",
                    "Relational database — ideal for structured data, transactions, and JPA/Hibernate integration");
            }
            if (reqs.contains("cache") || reqs.contains("redis") || reqs.contains("session")) {
                recommendations.put("Redis",
                    "In-memory cache and session store — enables fast caching, rate limiting, and session management");
            }
            if (reqs.contains("messag") || reqs.contains("queue") || reqs.contains("rabbit") || reqs.contains("async")) {
                recommendations.put("RabbitMQ",
                    "Message broker — enables async processing, event-driven architecture, and task queues");
            }
            if (reqs.contains("ai") || reqs.contains("llm") || reqs.contains("genai") || reqs.contains("ml")) {
                recommendations.put("GenAI on Tanzu Platform",
                    "LLM and embedding models — provides chat completion and vector embedding APIs");
            }
            if (reqs.contains("storage") || reqs.contains("s3") || reqs.contains("blob") || reqs.contains("file")) {
                recommendations.put("S3-compatible Object Storage",
                    "Binary/file storage — for artifacts, uploads, and static assets");
            }
            if (reqs.contains("auth") || reqs.contains("sso") || reqs.contains("oauth") || reqs.contains("identity")) {
                recommendations.put("p-identity (CF SSO)",
                    "OAuth2/OIDC identity provider — single sign-on, user management, and token-based auth");
            }
        }

        // Add language-specific defaults if no matches
        if (recommendations.isEmpty()) {
            recommendations.put("PostgreSQL", "Default relational database for most applications");
            if ("spring-boot".equalsIgnoreCase(framework) || "JAVA".equalsIgnoreCase(language)) {
                recommendations.put("Redis", "Session and cache management for Spring Boot");
                recommendations.put("RabbitMQ", "Event-driven messaging for Spring Cloud Stream");
            }
            if ("NODEJS".equalsIgnoreCase(language) || "express".equalsIgnoreCase(framework)) {
                recommendations.put("Redis", "Session store and caching for Node.js apps");
            }
        }

        for (var entry : recommendations.entrySet()) {
            sb.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    @Tool(description = "List services currently bound to a CF application by its app GUID")
    public String listBoundServices(@ToolParam(description = "CF application GUID") String appGuid) {
        try {
            String envJson = cfApiClient.get()
                .uri("/apps/{guid}/env", appGuid)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (envJson != null && envJson.contains("VCAP_SERVICES")) {
                return "Bound services for app " + appGuid + ":\n" + envJson;
            }
            return "No bound services found for app " + appGuid;
        } catch (Exception e) {
            log.warn("Failed to list bound services for app {}: {}", appGuid, e.getMessage());
            return "Unable to retrieve bound services for app " + appGuid +
                   ". The app may not be deployed or the CF API is unavailable.";
        }
    }

    @Tool(description = "Get a step-by-step guide for provisioning and binding a CF marketplace service to an application")
    public String getServiceProvisioningGuide(
            @ToolParam(description = "Service type (e.g., 'postgresql', 'redis', 'rabbitmq')") String serviceType,
            @ToolParam(description = "Programming language of the app") String language) {
        String serviceName = serviceType.toLowerCase();

        StringBuilder guide = new StringBuilder();
        guide.append("## Provisioning ").append(serviceType).append(" on Cloud Foundry\n\n");

        // Service-specific creation command
        switch (serviceName) {
            case "postgresql", "postgres" -> {
                guide.append("1. Create service instance:\n");
                guide.append("   `cf create-service postgresql small my-app-db`\n\n");
                guide.append("2. Bind to application:\n");
                guide.append("   `cf bind-service my-app my-app-db`\n\n");
                guide.append("3. Restage application:\n");
                guide.append("   `cf restage my-app`\n\n");
                appendLanguageBindingGuide(guide, "postgresql", language);
            }
            case "redis" -> {
                guide.append("1. Create service instance:\n");
                guide.append("   `cf create-service redis small my-app-cache`\n\n");
                guide.append("2. Bind to application:\n");
                guide.append("   `cf bind-service my-app my-app-cache`\n\n");
                guide.append("3. Restage application:\n");
                guide.append("   `cf restage my-app`\n\n");
                appendLanguageBindingGuide(guide, "redis", language);
            }
            case "rabbitmq", "rabbit" -> {
                guide.append("1. Create service instance:\n");
                guide.append("   `cf create-service rabbitmq small my-app-mq`\n\n");
                guide.append("2. Bind to application:\n");
                guide.append("   `cf bind-service my-app my-app-mq`\n\n");
                guide.append("3. Restage application:\n");
                guide.append("   `cf restage my-app`\n\n");
                appendLanguageBindingGuide(guide, "rabbitmq", language);
            }
            default -> {
                guide.append("1. List available plans:\n");
                guide.append("   `cf marketplace -e ").append(serviceType).append("`\n\n");
                guide.append("2. Create service instance:\n");
                guide.append("   `cf create-service ").append(serviceType).append(" <plan> my-app-").append(serviceName).append("`\n\n");
                guide.append("3. Bind to application:\n");
                guide.append("   `cf bind-service my-app my-app-").append(serviceName).append("`\n\n");
                guide.append("4. Restage application:\n");
                guide.append("   `cf restage my-app`\n");
            }
        }

        return guide.toString();
    }

    private void appendLanguageBindingGuide(StringBuilder guide, String service, String language) {
        if (language == null) return;
        String lang = language.toUpperCase();

        guide.append("### ").append(language).append(" Integration\n\n");

        if ("JAVA".equals(lang) && "postgresql".equals(service)) {
            guide.append("Add `java-cfenv-boot` to auto-configure the datasource from VCAP_SERVICES:\n");
            guide.append("```xml\n<dependency>\n  <groupId>io.pivotal.cfenv</groupId>\n");
            guide.append("  <artifactId>java-cfenv-boot</artifactId>\n</dependency>\n```\n");
            guide.append("Spring Boot will auto-detect the bound PostgreSQL and configure the DataSource.\n");
        } else if ("JAVA".equals(lang) && "redis".equals(service)) {
            guide.append("Add `spring-boot-starter-data-redis` and `java-cfenv-boot`.\n");
            guide.append("Spring Boot auto-configures RedisConnectionFactory from VCAP_SERVICES.\n");
        } else if ("JAVA".equals(lang) && "rabbitmq".equals(service)) {
            guide.append("Add `spring-boot-starter-amqp` or `spring-cloud-stream-binder-rabbit`.\n");
            guide.append("Spring Boot auto-configures RabbitMQ connection from VCAP_SERVICES.\n");
        } else if ("NODEJS".equals(lang)) {
            guide.append("Parse `process.env.VCAP_SERVICES` to get connection credentials:\n");
            guide.append("```javascript\nconst vcap = JSON.parse(process.env.VCAP_SERVICES);\n");
            guide.append("const creds = vcap['").append(service).append("'][0].credentials;\n```\n");
        } else if ("PYTHON".equals(lang)) {
            guide.append("Use `cfenv` package or parse `VCAP_SERVICES` environment variable:\n");
            guide.append("```python\nimport json, os\n");
            guide.append("vcap = json.loads(os.environ.get('VCAP_SERVICES', '{}'))\n");
            guide.append("creds = vcap.get('").append(service).append("', [{}])[0].get('credentials', {})\n```\n");
        }
    }
}
