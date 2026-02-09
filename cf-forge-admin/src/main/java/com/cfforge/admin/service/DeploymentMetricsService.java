package com.cfforge.admin.service;

import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.repository.DeploymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeploymentMetricsService {

    private final DeploymentRepository deploymentRepository;

    public DeploymentMetricsService(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    public Map<String, Object> getDeploymentMetrics() {
        List<Deployment> all = deploymentRepository.findAll();

        long total = all.size();
        long deployed = all.stream().filter(d -> d.getStatus() == DeployStatus.DEPLOYED).count();
        long failed = all.stream().filter(d -> d.getStatus() == DeployStatus.FAILED).count();
        long rolledBack = all.stream().filter(d -> d.getStatus() == DeployStatus.ROLLED_BACK).count();
        double successRate = total > 0 ? (double) deployed / total * 100 : 0;

        Map<String, Long> byStrategy = all.stream()
            .filter(d -> d.getStrategy() != null)
            .collect(Collectors.groupingBy(d -> d.getStrategy().name(), Collectors.counting()));

        Map<String, Long> byEnvironment = all.stream()
            .filter(d -> d.getEnvironment() != null)
            .collect(Collectors.groupingBy(d -> d.getEnvironment().name(), Collectors.counting()));

        return Map.of(
            "totalDeploys", total,
            "successfulDeploys", deployed,
            "failedDeploys", failed,
            "rollbacks", rolledBack,
            "successRate", Math.round(successRate * 10) / 10.0,
            "byStrategy", byStrategy,
            "byEnvironment", byEnvironment
        );
    }
}
