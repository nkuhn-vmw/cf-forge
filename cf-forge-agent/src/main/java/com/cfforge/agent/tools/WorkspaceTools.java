package com.cfforge.agent.tools;

import com.cfforge.common.dto.FileEntry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
public class WorkspaceTools {

    private final WebClient workspaceClient;

    public WorkspaceTools(WebClient workspaceClient) {
        this.workspaceClient = workspaceClient;
    }

    @Tool(description = "Read the content of a file in the project workspace")
    public String readFile(
            @ToolParam(description = "Project workspace ID") String workspaceId,
            @ToolParam(description = "File path relative to project root") String path) {
        return workspaceClient.get()
            .uri("/workspace/{id}/files/{path}", workspaceId, path)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Tool(description = "Write or create a file in the project workspace")
    public String writeFile(
            @ToolParam(description = "Project workspace ID") String workspaceId,
            @ToolParam(description = "File path relative to project root") String path,
            @ToolParam(description = "File content") String content) {
        workspaceClient.put()
            .uri("/workspace/{id}/files/{path}", workspaceId, path)
            .bodyValue(content)
            .retrieve()
            .toBodilessEntity()
            .block();
        return "File written: " + path;
    }

    @Tool(description = "List all files in the project workspace")
    public String listFiles(
            @ToolParam(description = "Project workspace ID") String workspaceId,
            @ToolParam(description = "Directory path, defaults to root", required = false) String dir) {
        return workspaceClient.get()
            .uri("/workspace/{id}/files?dir={dir}", workspaceId, dir != null ? dir : "")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Tool(description = "Delete a file from the project workspace")
    public String deleteFile(
            @ToolParam(description = "Project workspace ID") String workspaceId,
            @ToolParam(description = "File path to delete") String path) {
        workspaceClient.delete()
            .uri("/workspace/{id}/files/{path}", workspaceId, path)
            .retrieve()
            .toBodilessEntity()
            .block();
        return "File deleted: " + path;
    }
}
