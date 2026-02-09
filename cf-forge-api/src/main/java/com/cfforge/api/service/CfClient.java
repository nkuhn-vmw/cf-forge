package com.cfforge.api.service;

import com.cfforge.api.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CfClient {

    private final WebClient cfApiClient;

    public CfClient(@Qualifier("cfApiWebClient") WebClient cfApiClient) {
        this.cfApiClient = cfApiClient;
    }

    public Mono<CfApp> getApp(String appGuid) {
        return cfApiClient.get().uri("/apps/{guid}", appGuid)
            .retrieve().bodyToMono(CfApp.class);
    }

    public Flux<ServiceOffering> listMarketplace() {
        return cfApiClient.get().uri("/service_offerings")
            .retrieve().bodyToMono(new ParameterizedTypeReference<CfPaginatedResponse<ServiceOffering>>() {})
            .flatMapMany(r -> Flux.fromIterable(r.resources()));
    }

    public Flux<BuildpackInfo> listBuildpacks() {
        return cfApiClient.get().uri("/buildpacks")
            .retrieve().bodyToMono(new ParameterizedTypeReference<CfPaginatedResponse<BuildpackInfo>>() {})
            .flatMapMany(r -> Flux.fromIterable(r.resources()));
    }

    public Mono<Void> scaleApp(String appGuid, int instances, String memory) {
        return cfApiClient.patch().uri("/apps/{guid}/processes/web", appGuid)
            .bodyValue(Map.of("instances", instances, "memory_in_mb", parseMemory(memory)))
            .retrieve().toBodilessEntity().then();
    }

    public Mono<CfApp> createApp(String name, String spaceGuid) {
        return cfApiClient.post().uri("/apps")
            .bodyValue(Map.of(
                "name", name,
                "relationships", Map.of("space", Map.of("data", Map.of("guid", spaceGuid)))
            ))
            .retrieve().bodyToMono(CfApp.class);
    }

    public Mono<CfRoute> createRoute(String domainGuid, String host, String spaceGuid) {
        return cfApiClient.post().uri("/routes")
            .bodyValue(Map.of(
                "host", host,
                "relationships", Map.of(
                    "domain", Map.of("data", Map.of("guid", domainGuid)),
                    "space", Map.of("data", Map.of("guid", spaceGuid))
                )
            ))
            .retrieve().bodyToMono(CfRoute.class);
    }

    public Mono<Void> mapRoute(String routeGuid, String appGuid) {
        return cfApiClient.post().uri("/routes/{routeGuid}/destinations", routeGuid)
            .bodyValue(Map.of("destinations", List.of(
                Map.of("app", Map.of("guid", appGuid))
            )))
            .retrieve().toBodilessEntity().then();
    }

    public Mono<Void> bindService(String appGuid, String serviceInstanceGuid) {
        return cfApiClient.post().uri("/service_credential_bindings")
            .bodyValue(Map.of(
                "type", "app",
                "relationships", Map.of(
                    "app", Map.of("data", Map.of("guid", appGuid)),
                    "service_instance", Map.of("data", Map.of("guid", serviceInstanceGuid))
                )
            ))
            .retrieve().toBodilessEntity().then();
    }

    public Mono<String> getRecentLogs(String appGuid, int lines) {
        return cfApiClient.get().uri(uriBuilder -> uriBuilder
                .path("/apps/{guid}/env")
                .build(appGuid))
            .retrieve().bodyToMono(String.class);
    }

    public Mono<OrgQuota> getOrgQuota(String orgGuid) {
        return cfApiClient.get().uri("/organization_quotas/{guid}", orgGuid)
            .retrieve().bodyToMono(OrgQuota.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getUserSpaceRoles(String token, String spaceGuid) {
        try {
            var response = cfApiClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/roles")
                    .queryParam("space_guids", spaceGuid)
                    .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            Set<String> roles = new HashSet<>();
            if (response != null && response.get("resources") instanceof List<?> resources) {
                for (Object r : resources) {
                    if (r instanceof Map<?, ?> role) {
                        roles.add((String) role.get("type"));
                    }
                }
            }
            return roles;
        } catch (Exception e) {
            return Set.of();
        }
    }

    private int parseMemory(String memory) {
        if (memory == null) return 1024;
        String upper = memory.toUpperCase().trim();
        if (upper.endsWith("G")) return (int) (Double.parseDouble(upper.replace("G", "")) * 1024);
        if (upper.endsWith("M")) return Integer.parseInt(upper.replace("M", ""));
        return Integer.parseInt(upper);
    }
}
