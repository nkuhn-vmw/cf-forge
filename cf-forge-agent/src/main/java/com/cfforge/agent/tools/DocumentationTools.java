package com.cfforge.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DocumentationTools {

    private final VectorStore vectorStore;

    public DocumentationTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search CF documentation and best practices for a given topic. " +
                        "Use this when you need to reference Cloud Foundry documentation, buildpack docs, " +
                        "or platform-specific guidance.")
    public String searchDocumentation(String query) {
        var results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.7)
                .build()
        );

        if (results.isEmpty()) {
            return "No relevant documentation found for: " + query;
        }

        return results.stream()
            .map(doc -> "---\n" + doc.getText() + "\nSource: " + doc.getMetadata().getOrDefault("source", "unknown"))
            .collect(Collectors.joining("\n\n"));
    }

    @Tool(description = "Get CF buildpack documentation and configuration for a specific language/runtime.")
    public String getBuildpackDocs(String language) {
        var results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(language + " buildpack configuration CF Cloud Foundry")
                .topK(3)
                .similarityThreshold(0.6)
                .build()
        );

        if (results.isEmpty()) {
            return getDefaultBuildpackInfo(language);
        }

        return results.stream()
            .map(doc -> doc.getText())
            .collect(Collectors.joining("\n\n"));
    }

    private String getDefaultBuildpackInfo(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "Use java_buildpack_offline. Set JBP_CONFIG_OPEN_JDK_JRE for Java version. " +
                          "Spring Boot apps detected automatically. Use Actuator for health checks.";
            case "nodejs", "node" -> "Use nodejs_buildpack. package.json scripts.start defines entry point. " +
                          "Set NODE_ENV=production. Use .cfignore for node_modules.";
            case "python" -> "Use python_buildpack. Procfile defines web process. " +
                          "requirements.txt for dependencies. runtime.txt for Python version.";
            case "go" -> "Use go_buildpack. go.mod required. Set GOPACKAGENAME if needed.";
            case "staticfile", "static" -> "Use staticfile_buildpack. Staticfile config file for nginx. " +
                          "pushstate: enabled for SPAs. root: . for current directory.";
            default -> "Check CF docs for " + language + " buildpack configuration.";
        };
    }
}
