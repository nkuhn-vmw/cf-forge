package com.cfforge.admin.service;

import com.cfforge.common.entity.Deployment;
import com.cfforge.common.enums.DeployEnvironment;
import com.cfforge.common.enums.DeployStatus;
import com.cfforge.common.enums.DeployStrategy;
import com.cfforge.common.repository.DeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentMetricsServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    private DeploymentMetricsService service;

    @BeforeEach
    void setUp() {
        service = new DeploymentMetricsService(deploymentRepository);
    }

    @Test
    void getDeploymentMetrics_withDeployments_calculatesCorrectly() {
        Deployment deployed = Deployment.builder()
            .status(DeployStatus.DEPLOYED)
            .strategy(DeployStrategy.ROLLING)
            .environment(DeployEnvironment.PRODUCTION)
            .build();
        Deployment failed = Deployment.builder()
            .status(DeployStatus.FAILED)
            .strategy(DeployStrategy.BLUE_GREEN)
            .environment(DeployEnvironment.STAGING)
            .build();
        Deployment rolledBack = Deployment.builder()
            .status(DeployStatus.ROLLED_BACK)
            .strategy(DeployStrategy.ROLLING)
            .environment(DeployEnvironment.PRODUCTION)
            .build();
        when(deploymentRepository.findAll()).thenReturn(List.of(deployed, failed, rolledBack));

        Map<String, Object> result = service.getDeploymentMetrics();

        assertThat(result.get("totalDeploys")).isEqualTo(3L);
        assertThat(result.get("successfulDeploys")).isEqualTo(1L);
        assertThat(result.get("failedDeploys")).isEqualTo(1L);
        assertThat(result.get("rollbacks")).isEqualTo(1L);
        assertThat(result.get("successRate")).isEqualTo(33.3);
    }

    @Test
    void getDeploymentMetrics_withNoDeployments_returnsZeros() {
        when(deploymentRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.getDeploymentMetrics();

        assertThat(result.get("totalDeploys")).isEqualTo(0L);
        assertThat(result.get("successfulDeploys")).isEqualTo(0L);
        assertThat(result.get("failedDeploys")).isEqualTo(0L);
        assertThat(result.get("rollbacks")).isEqualTo(0L);
        assertThat(result.get("successRate")).isEqualTo(0.0);
    }

    @Test
    void getDeploymentMetrics_groupsByStrategy() {
        Deployment rolling = Deployment.builder()
            .status(DeployStatus.DEPLOYED).strategy(DeployStrategy.ROLLING)
            .environment(DeployEnvironment.STAGING).build();
        Deployment blueGreen = Deployment.builder()
            .status(DeployStatus.DEPLOYED).strategy(DeployStrategy.BLUE_GREEN)
            .environment(DeployEnvironment.PRODUCTION).build();
        when(deploymentRepository.findAll()).thenReturn(List.of(rolling, blueGreen));

        Map<String, Object> result = service.getDeploymentMetrics();

        @SuppressWarnings("unchecked")
        Map<String, Long> byStrategy = (Map<String, Long>) result.get("byStrategy");
        assertThat(byStrategy).containsEntry("ROLLING", 1L);
        assertThat(byStrategy).containsEntry("BLUE_GREEN", 1L);
    }

    @Test
    void getDeploymentMetrics_groupsByEnvironment() {
        Deployment staging = Deployment.builder()
            .status(DeployStatus.DEPLOYED).strategy(DeployStrategy.ROLLING)
            .environment(DeployEnvironment.STAGING).build();
        Deployment prod = Deployment.builder()
            .status(DeployStatus.DEPLOYED).strategy(DeployStrategy.ROLLING)
            .environment(DeployEnvironment.PRODUCTION).build();
        when(deploymentRepository.findAll()).thenReturn(List.of(staging, prod));

        Map<String, Object> result = service.getDeploymentMetrics();

        @SuppressWarnings("unchecked")
        Map<String, Long> byEnv = (Map<String, Long>) result.get("byEnvironment");
        assertThat(byEnv).containsEntry("STAGING", 1L);
        assertThat(byEnv).containsEntry("PRODUCTION", 1L);
    }
}
