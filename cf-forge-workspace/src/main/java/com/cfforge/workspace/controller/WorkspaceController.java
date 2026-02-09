package com.cfforge.workspace.controller;

import com.cfforge.common.dto.FileEntry;
import com.cfforge.workspace.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/workspace/{workspaceId}")
public class WorkspaceController {

    private final FileStorageService fileStorageService;

    public WorkspaceController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/files")
    public List<FileEntry> listFiles(@PathVariable UUID workspaceId,
                                      @RequestParam(defaultValue = "") String dir) {
        return fileStorageService.listFiles(workspaceId, dir);
    }

    @GetMapping("/files/{*path}")
    public ResponseEntity<String> readFile(@PathVariable UUID workspaceId,
                                            @PathVariable String path) {
        String content = fileStorageService.readFile(workspaceId, path);
        return ResponseEntity.ok(content);
    }

    @PutMapping("/files/{*path}")
    public ResponseEntity<Void> writeFile(@PathVariable UUID workspaceId,
                                           @PathVariable String path,
                                           @RequestBody Map<String, String> body) {
        fileStorageService.writeFile(workspaceId, path, body.get("content"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/files/{*path}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID workspaceId,
                                            @PathVariable String path) {
        fileStorageService.deleteFile(workspaceId, path);
        return ResponseEntity.noContent().build();
    }
}
