package com.cfforge.admin.service;

import com.cfforge.admin.config.HealthCheckProperties;
import com.cfforge.common.entity.ComponentHealthHistory;
import com.cfforge.common.enums.HealthStatus;
import com.cfforge.common.repository.ComponentHealthHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HealthCheckScheduler {

    private final ComponentHealthHistoryRepository healthRepository;
    private final WebClient.Builder webClientBuilder;
    private final HealthCheckProperties healthCheckProperties;

    public HealthCheckScheduler(ComponentHealthHistoryRepository healthRepository,
                                 WebClient.Builder webClientBuilder,
                                 HealthCheckProperties healthCheckProperties) {
        this.healthRepository = healthRepository;
        this.webClientBuilder = webClientBuilder;
        this.healthCheckProperties = healthCheckProperties;
    }

    @Scheduled(fixedRate = 60_000)
    public void checkComponentHealth() {
        healthCheckProperties.getComponents().forEach((component, url) -> {
            try {
                var response = webClientBuilder.build()
                    .get().uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(5));

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
