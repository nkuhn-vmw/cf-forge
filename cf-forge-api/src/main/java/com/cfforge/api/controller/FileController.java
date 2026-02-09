package com.cfforge.api.controller;

import com.cfforge.common.dto.FileEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/files")
public class FileController {

    private final WebClient workspaceClient;

    public FileController(@Qualifier("workspaceWebClient") WebClient workspaceClient) {
        this.workspaceClient = workspaceClient;
    }

    @GetMapping
    public List<FileEntry> listFiles(@PathVariable UUID projectId,
                                      @RequestParam(defaultValue = "") String dir) {
        return workspaceClient.get()
            .uri("/workspace/{projectId}/files?dir={dir}", projectId, dir)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<FileEntry>>() {})
            .block();
    }

    @GetMapping("/**")
    public ResponseEntity<String> readFile(@PathVariable UUID projectId, HttpServletRequest request) {
        String path = extractFilePath(request, projectId);
        String content = workspaceClient.get()
            .uri("/workspace/{projectId}/files/{path}", projectId, path)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        return ResponseEntity.ok(content);
    }

    @PutMapping("/**")
    public ResponseEntity<Void> writeFile(@PathVariable UUID projectId,
                                           HttpServletRequest request,
                                           @RequestBody String content) {
        String path = extractFilePath(request, projectId);
        workspaceClient.put()
            .uri("/workspace/{projectId}/files/{path}", projectId, path)
            .bodyValue(Map.of("content", content))
            .retrieve()
            .toBodilessEntity()
            .block();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/**")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID projectId, HttpServletRequest request) {
        String path = extractFilePath(request, projectId);
        workspaceClient.delete()
            .uri("/workspace/{projectId}/files/{path}", projectId, path)
            .retrieve()
            .toBodilessEntity()
            .block();
        return ResponseEntity.noContent().build();
    }

    private String extractFilePath(HttpServletRequest request, UUID projectId) {
        String prefix = "/api/v1/projects/" + projectId + "/files/";
        return request.getRequestURI().substring(prefix.length());
    }
}
