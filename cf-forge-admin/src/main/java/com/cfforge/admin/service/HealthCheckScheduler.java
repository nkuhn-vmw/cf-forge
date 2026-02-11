package com.cfforge.admin.service;

import com.cfforge.common.entity.ComponentHealthHistory;
import com.cfforge.common.enums.HealthStatus;
import com.cfforge.common.repository.ComponentHealthHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HealthCheckScheduler {

    private final ComponentHealthHistoryRepository healthRepository;
    private final WebClient.Builder webClientBuilder;

    private static final Map<String, String> COMPONENTS = Map.of(
        "cf-forge-api", "https://cf-forge-api.apps.tas-ndc.kuhn-labs.com/actuator/health",
        "cf-forge-agent", "http://cf-forge-agent.apps.internal:8080/actuator/health",
        "cf-forge-builder", "http://cf-forge-builder.apps.internal:8080/actuator/health",
        "cf-forge-workspace", "http://cf-forge-workspace.apps.internal:8080/actuator/health"
    );

    public HealthCheckScheduler(ComponentHealthHistoryRepository healthRepository,
                                 WebClient.Builder webClientBuilder) {
        this.healthRepository = healthRepository;
        this.webClientBuilder = webClientBuilder;
    }

    @Scheduled(fixedRate = 60_000) // Every minute
    public void checkComponentHealth() {
        COMPONENTS.forEach((component, url) -> {
            try {
                var response = webClientBuilder.build()
                    .get().uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(5));

                HealthStatus status = HealthStatus.UP;
                if (response != null && "DOWN".equals(response.get("status"))) {
                    status = HealthStatus.DOWN;
                }

                saveHealth(component, status, null);
            } catch (Exception e) {
                log.warn("Health check failed for {}: {}", component, e.getMessage());
                saveHealth(component, HealthStatus.DOWN, e.getMessage());
            }
        });
    }

    private void saveHealth(String component, HealthStatus status, String details) {
        var health = new ComponentHealthHistory();
        health.setComponentName(component);
        health.setStatus(status);
        if (details != null) {
            health.setDetails(Map.of("error", details));
        }
        healthRepository.save(health);
    }

    public List<Object[]> getLatestHealth() {
        return healthRepository.findLatestHealthForAllComponents();
    }
}
