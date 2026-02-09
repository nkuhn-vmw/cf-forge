package com.cfforge.admin.service;

import com.cfforge.common.entity.Build;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.repository.BuildRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BuildMetricsService {

    private final BuildRepository buildRepository;

    public BuildMetricsService(BuildRepository buildRepository) {
        this.buildRepository = buildRepository;
    }

    public Map<String, Object> getBuildMetrics() {
        List<Build> allBuilds = buildRepository.findAll();

        long total = allBuilds.size();
        long successful = allBuilds.stream().filter(b -> b.getStatus() == BuildStatus.SUCCESS).count();
        long failed = allBuilds.stream().filter(b -> b.getStatus() == BuildStatus.FAILED).count();
        double successRate = total > 0 ? (double) successful / total * 100 : 0;
        double avgDuration = allBuilds.stream()
            .filter(b -> b.getDurationMs() != null && b.getDurationMs() > 0)
            .mapToInt(Build::getDurationMs)
            .average()
            .orElse(0);

        Map<String, Long> byStatus = allBuilds.stream()
            .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        return Map.of(
            "totalBuilds", total,
            "successfulBuilds", successful,
            "failedBuilds", failed,
            "successRate", Math.round(successRate * 10) / 10.0,
            "avgDurationMs", Math.round(avgDuration),
            "byStatus", byStatus
        );
    }
}
