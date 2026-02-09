package com.cfforge.api.controller;

import com.cfforge.common.entity.CfTarget;
import com.cfforge.common.repository.CfTargetRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-foundation deployment support (ECO-003).
 *
 * Manages CF foundation targets (API endpoints, orgs, spaces) for deploying
 * applications across multiple Cloud Foundry installations. Users can register
 * multiple foundations and select which one to deploy to per project.
 */
@RestController
@RequestMapping("/api/v1/targets")
public class CfTargetController {

    private final CfTargetRepository cfTargetRepository;

    public CfTargetController(CfTargetRepository cfTargetRepository) {
        this.cfTargetRepository = cfTargetRepository;
    }

    @GetMapping
    public List<CfTarget> listTargets() {
        return cfTargetRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CfTarget> getTarget(@PathVariable UUID id) {
        return cfTargetRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CfTarget createTarget(@RequestBody CfTarget target) {
        return cfTargetRepository.save(target);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CfTarget> updateTarget(@PathVariable UUID id,
                                                    @RequestBody CfTarget update) {
        return cfTargetRepository.findById(id)
            .map(existing -> {
                existing.setApiEndpoint(update.getApiEndpoint());
                existing.setOrgGuid(update.getOrgGuid());
                existing.setOrgName(update.getOrgName());
                existing.setSpaceGuid(update.getSpaceGuid());
                existing.setSpaceName(update.getSpaceName());
                existing.setDefault(update.isDefault());
                return ResponseEntity.ok(cfTargetRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(@PathVariable UUID id) {
        cfTargetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    public CfTarget setDefault(@PathVariable UUID id) {
        // Clear existing defaults
        cfTargetRepository.findAll().forEach(t -> {
            if (t.isDefault()) {
                t.setDefault(false);
                cfTargetRepository.save(t);
            }
        });
        CfTarget target = cfTargetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Target not found: " + id));
        target.setDefault(true);
        return cfTargetRepository.save(target);
    }

    @PostMapping("/{id}/validate")
    public Map<String, Object> validateConnection(@PathVariable UUID id) {
        CfTarget target = cfTargetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Target not found: " + id));

        try {
            // Test connectivity by hitting the CF API info endpoint
            var client = org.springframework.web.reactive.function.client.WebClient.builder()
                .baseUrl(target.getApiEndpoint())
                .build();

            var info = client.get().uri("/v3/info")
                .retrieve()
                .bodyToMono(Map.class)
                .block(java.time.Duration.ofSeconds(10));

            return Map.of(
                "status", "connected",
                "apiVersion", info != null ? info.getOrDefault("api_version", "unknown") : "unknown",
                "foundation", target.getApiEndpoint(),
                "org", target.getOrgName() != null ? target.getOrgName() : "",
                "space", target.getSpaceName() != null ? target.getSpaceName() : ""
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "foundation", target.getApiEndpoint()
            );
        }
    }
}
