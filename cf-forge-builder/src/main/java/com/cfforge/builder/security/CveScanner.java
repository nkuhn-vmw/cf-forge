package com.cfforge.builder.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

@Component
@Slf4j
public class CveScanner {

    @Value("${cf.forge.cve.scan-enabled:true}")
    private boolean scanEnabled;

    @Value("${cf.forge.cve.block-severity:critical}")
    private String blockSeverity;

    public CveScanResult scan(Path projectDir) {
        if (!scanEnabled) {
            log.info("CVE scanning disabled, skipping");
            return CveScanResult.builder()
                .scanned(false)
                .vulnerabilities(List.of())
                .blocked(false)
                .summary("CVE scanning disabled")
                .build();
        }

        try {
            log.info("Running Trivy CVE scan on: {}", projectDir);
            ProcessBuilder pb = new ProcessBuilder(
                "trivy", "fs", "--format", "json", "--severity",
                "CRITICAL,HIGH,MEDIUM,LOW", projectDir.toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            List<Vulnerability> vulnerabilities = parseVulnerabilities(output.toString());
            boolean blocked = shouldBlock(vulnerabilities);

            Map<String, Integer> severityCounts = new LinkedHashMap<>();
            severityCounts.put("CRITICAL", 0);
            severityCounts.put("HIGH", 0);
            severityCounts.put("MEDIUM", 0);
            severityCounts.put("LOW", 0);
            for (Vulnerability v : vulnerabilities) {
                severityCounts.merge(v.severity().toUpperCase(), 1, Integer::sum);
            }

            String summary = String.format("Found %d vulnerabilities (C:%d H:%d M:%d L:%d)%s",
                vulnerabilities.size(),
                severityCounts.get("CRITICAL"),
                severityCounts.get("HIGH"),
                severityCounts.get("MEDIUM"),
                severityCounts.get("LOW"),
                blocked ? " — BLOCKED" : "");

            log.info("CVE scan complete: {}", summary);

            return CveScanResult.builder()
                .scanned(true)
                .vulnerabilities(vulnerabilities)
                .severityCounts(severityCounts)
                .blocked(blocked)
                .summary(summary)
                .exitCode(exitCode)
                .build();

        } catch (Exception e) {
            log.warn("Trivy not available or scan failed: {}", e.getMessage());
            return CveScanResult.builder()
                .scanned(false)
                .vulnerabilities(List.of())
                .blocked(false)
                .summary("CVE scan unavailable: " + e.getMessage())
                .build();
        }
    }

    private boolean shouldBlock(List<Vulnerability> vulnerabilities) {
        if ("none".equalsIgnoreCase(blockSeverity)) return false;

        List<String> blockLevels = switch (blockSeverity.toLowerCase()) {
            case "low" -> List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
            case "medium" -> List.of("MEDIUM", "HIGH", "CRITICAL");
            case "high" -> List.of("HIGH", "CRITICAL");
            case "critical" -> List.of("CRITICAL");
            default -> List.of("CRITICAL");
        };

        return vulnerabilities.stream()
            .anyMatch(v -> blockLevels.contains(v.severity().toUpperCase()));
    }

    private List<Vulnerability> parseVulnerabilities(String json) {
        List<Vulnerability> result = new ArrayList<>();
        try {
            // Simple JSON parsing for Trivy output format
            // Trivy outputs {"Results": [{"Vulnerabilities": [...]}]}
            int idx = json.indexOf("\"Vulnerabilities\"");
            if (idx < 0) return result;

            // Extract vulnerability IDs and severities via pattern matching
            String remaining = json.substring(idx);
            java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile(
                "\"VulnerabilityID\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"Severity\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"PkgName\"\\s*:\\s*\"([^\"]+)\"");
            var matcher = idPattern.matcher(remaining);
            while (matcher.find()) {
                result.add(new Vulnerability(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    ""
                ));
            }
        } catch (Exception e) {
            log.debug("Failed to parse Trivy JSON output", e);
        }
        return result;
    }

    public record Vulnerability(String id, String severity, String packageName, String fixedVersion) {}
}
