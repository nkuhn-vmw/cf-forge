package com.cfforge.api.service.deploy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Canary deployment strategy: deploys a single canary instance alongside
 * the existing fleet, routes a fraction of traffic to it, validates health,
 * then gradually shifts all traffic before removing old instances.
 */
@Service("canaryDeploy")
@Slf4j
public class CanaryDeployStrategy implements DeployStrategyInterface {

    @Override
    public DeployResult execute(DeployContext ctx) {
        long start = System.currentTimeMillis();
        StringBuilder deployLog = new StringBuilder();
        String appName = ctx.getAppName();
        String canaryName = appName + "-canary";

        try {
            // Step 1: Push canary with 1 instance
            deployLog.append("[1/5] Pushing canary instance: ").append(canaryName).append("\n");
            runCf(deployLog, "push", canaryName, "-f", "manifest.yml",
                "-i", "1", "--no-route");

            // Step 2: Map route to canary (traffic split)
            deployLog.append("[2/5] Mapping route to canary for traffic split\n");
            String domain = ctx.getDomain() != null ? ctx.getDomain() : "apps.internal";
            runCf(deployLog, "map-route", canaryName, domain, "--hostname", appName);

            // Step 3: Health check on canary
            deployLog.append("[3/5] Validating canary health...\n");
            Thread.sleep(15000); // Wait for canary to stabilize
            int canaryHealth = checkHealth(canaryName);
            if (canaryHealth != 0) {
                deployLog.append("Canary health check FAILED — rolling back\n");
                rollback(ctx);
                return DeployResult.builder()
                    .status("FAILED")
                    .log(deployLog.toString())
                    .errorMessage("Canary health check failed")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
            }
            deployLog.append("Canary health check passed\n");

            // Step 4: Scale up canary, scale down original
            deployLog.append("[4/5] Promoting canary — scaling up and replacing original\n");
            runCf(deployLog, "scale", canaryName, "-i", "2");
            Thread.sleep(10000);

            // Step 5: Remove old app, rename canary
            deployLog.append("[5/5] Removing old app and renaming canary\n");
            runCf(deployLog, "unmap-route", appName, domain, "--hostname", appName);
            runCf(deployLog, "delete", appName, "-f");
            runCf(deployLog, "rename", canaryName, appName);

            String deployUrl = "https://" + appName + "." + domain;
            return DeployResult.builder()
                .status("DEPLOYED")
                .deploymentUrl(deployUrl)
                .log(deployLog.toString())
                .durationMs(System.currentTimeMillis() - start)
                .build();

        } catch (Exception e) {
            deployLog.append("CANARY DEPLOY FAILED: ").append(e.getMessage()).append("\n");
            try { rollback(ctx); } catch (Exception ignored) {}
            return DeployResult.builder()
                .status("FAILED")
                .log(deployLog.toString())
                .errorMessage(e.getMessage())
                .durationMs(System.currentTimeMillis() - start)
                .build();
        }
    }

    @Override
    public void rollback(DeployContext ctx) {
        String canaryName = ctx.getAppName() + "-canary";
        log.info("Rolling back canary deployment: deleting {}", canaryName);
        try {
            runCf(new StringBuilder(), "delete", canaryName, "-f", "-r");
        } catch (Exception e) {
            log.warn("Canary rollback cleanup failed: {}", e.getMessage());
        }
    }

    @Override
    public String strategyName() {
        return "canary";
    }

    private int checkHealth(String appName) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cf", "ssh", appName,
            "-c", "curl -sf http://localhost:8080/actuator/health");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        return p.waitFor();
    }

    private void runCf(StringBuilder log, String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "cf";
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("cf " + String.join(" ", args) + " failed (exit " + exitCode + ")");
        }
    }
}
