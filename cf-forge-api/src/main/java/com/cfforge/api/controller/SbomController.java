package com.cfforge.api.controller;

import com.cfforge.common.repository.BuildRepository;
import com.cfforge.common.storage.S3StorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/builds/{buildId}")
public class SbomController {

    private final BuildRepository buildRepository;
    private final S3StorageService storageService;

    public SbomController(BuildRepository buildRepository, S3StorageService storageService) {
        this.buildRepository = buildRepository;
        this.storageService = storageService;
    }

    @GetMapping("/sbom")
    public ResponseEntity<byte[]> downloadSbom(@PathVariable UUID projectId, @PathVariable UUID buildId) {
        var build = buildRepository.findById(buildId)
            .orElseThrow(() -> new RuntimeException("Build not found"));

        if (build.getSbomPath() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = storageService.getObject(build.getSbomPath());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sbom-" + buildId + ".json")
            .contentType(MediaType.APPLICATION_JSON)
            .body(content);
    }

    @GetMapping("/cve-report")
    public ResponseEntity<?> getCveReport(@PathVariable UUID projectId, @PathVariable UUID buildId) {
        var build = buildRepository.findById(buildId)
            .orElseThrow(() -> new RuntimeException("Build not found"));

        if (build.getCveReport() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(build.getCveReport());
    }
}
