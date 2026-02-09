package com.cfforge.builder.supplychain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TAP (Tanzu Application Platform) Supply Chain Integration (ECO-006).
 *
 * Integrates CF Forge build pipeline with TAP's supply chain choreographer.
 * Enables source-to-URL workflows via TAP's Cartographer, including
 * automated testing, scanning, and image building through TAP's
 * supply chain security features.
 */
@Service
@Slf4j
public class TapSupplyChainIntegration {

    @Value("${cf.forge.tap.api-url:}")
    private String tapApiUrl;

    @Value("${cf.forge.tap.namespace:default}")
    private String tapNamespace;

    private final WebClient.Builder webClientBuilder;

    public TapSupplyChainIntegration(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Submit a workload to TAP supply chain.
     */
    public SupplyChainSubmission submitWorkload(String projectName, String gitUrl,
                                                  String branch, String language) {
        if (tapApiUrl == null || tapApiUrl.isBlank()) {
            log.info("TAP integration disabled (no API URL configured)");
            return SupplyChainSubmission.builder()
                .status("SKIPPED")
                .message("TAP integration not configured")
                .build();
        }

        WebClient client = webClientBuilder.baseUrl(tapApiUrl).build();

        Map<String, Object> workload = Map.of(
            "apiVersion", "carto.run/v1alpha1",
            "kind", "Workload",
            "metadata", Map.of(
                "name", projectName,
                "namespace", tapNamespace,
                "labels", Map.of(
                    "apps.tanzu.vmware.com/workload-type", "web",
                    "app.kubernetes.io/part-of", projectName
                )
            ),
            "spec", Map.of(
                "source", Map.of(
                    "git", Map.of(
                        "url", gitUrl,
                        "ref", Map.of("branch", branch)
                    )
                ),
                "build", Map.of(
                    "env", List.of(
                        Map.of("name", "BP_JVM_VERSION", "value", "21")
                    )
                )
            )
        );

        try {
            var response = client.post()
                .uri("/apis/carto.run/v1alpha1/namespaces/{ns}/workloads", tapNamespace)
                .bodyValue(workload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            log.info("TAP workload submitted: {}", projectName);
            return SupplyChainSubmission.builder()
                .status("SUBMITTED")
                .workloadName(projectName)
                .namespace(tapNamespace)
                .message("Workload submitted to TAP supply chain")
                .build();
        } catch (Exception e) {
            log.error("TAP submission failed: {}", e.getMessage());
            return SupplyChainSubmission.builder()
                .status("FAILED")
                .message(e.getMessage())
                .build();
        }
    }

    /**
     * Check supply chain status for a workload.
     */
    public Map<String, Object> getWorkloadStatus(String workloadName) {
        if (tapApiUrl == null || tapApiUrl.isBlank()) {
            return Map.of("status", "TAP not configured");
        }

        WebClient client = webClientBuilder.baseUrl(tapApiUrl).build();
        try {
            return client.get()
                .uri("/apis/carto.run/v1alpha1/namespaces/{ns}/workloads/{name}",
                    tapNamespace, workloadName)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @Data
    @lombok.Builder
    public static class SupplyChainSubmission {
        private String status;
        private String workloadName;
        private String namespace;
        private String message;
    }
}
