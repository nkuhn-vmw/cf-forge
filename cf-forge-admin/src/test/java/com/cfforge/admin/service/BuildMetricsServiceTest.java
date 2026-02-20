package com.cfforge.admin.service;

import com.cfforge.common.entity.Build;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.repository.BuildRepository;
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
class BuildMetricsServiceTest {

    @Mock
    private BuildRepository buildRepository;

    private BuildMetricsService service;

    @BeforeEach
    void setUp() {
        service = new BuildMetricsService(buildRepository);
    }

    @Test
    void getBuildMetrics_withBuilds_calculatesCorrectly() {
        Build success1 = Build.builder().status(BuildStatus.SUCCESS).durationMs(1000).build();
        Build success2 = Build.builder().status(BuildStatus.SUCCESS).durationMs(3000).build();
        Build failed = Build.builder().status(BuildStatus.FAILED).durationMs(500).build();
        when(buildRepository.findAll()).thenReturn(List.of(success1, success2, failed));

        Map<String, Object> result = service.getBuildMetrics();

        assertThat(result.get("totalBuilds")).isEqualTo(3L);
        assertThat(result.get("successfulBuilds")).isEqualTo(2L);
        assertThat(result.get("failedBuilds")).isEqualTo(1L);
        assertThat(result.get("successRate")).isEqualTo(66.7);
        assertThat(result.get("avgDurationMs")).isEqualTo(1500L);
    }

    @Test
    void getBuildMetrics_withNoBuilds_returnsZeros() {
        when(buildRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.getBuildMetrics();

        assertThat(result.get("totalBuilds")).isEqualTo(0L);
        assertThat(result.get("successfulBuilds")).isEqualTo(0L);
        assertThat(result.get("failedBuilds")).isEqualTo(0L);
        assertThat(result.get("successRate")).isEqualTo(0.0);
        assertThat(result.get("avgDurationMs")).isEqualTo(0L);
    }

    @Test
    void getBuildMetrics_groupsByStatus() {
        Build success = Build.builder().status(BuildStatus.SUCCESS).durationMs(1000).build();
        Build queued = Build.builder().status(BuildStatus.QUEUED).durationMs(null).build();
        when(buildRepository.findAll()).thenReturn(List.of(success, queued));

        Map<String, Object> result = service.getBuildMetrics();

        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) result.get("byStatus");
        assertThat(byStatus).containsEntry("SUCCESS", 1L);
        assertThat(byStatus).containsEntry("QUEUED", 1L);
    }

    @Test
    void getBuildMetrics_skipsNullDurations_inAverage() {
        Build withDuration = Build.builder().status(BuildStatus.SUCCESS).durationMs(2000).build();
        Build noDuration = Build.builder().status(BuildStatus.SUCCESS).durationMs(null).build();
        when(buildRepository.findAll()).thenReturn(List.of(withDuration, noDuration));

        Map<String, Object> result = service.getBuildMetrics();

        assertThat(result.get("avgDurationMs")).isEqualTo(2000L);
    }
}
