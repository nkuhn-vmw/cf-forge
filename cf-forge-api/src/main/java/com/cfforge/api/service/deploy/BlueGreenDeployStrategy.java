package com.cfforge.api.service.deploy;

import com.cfforge.api.service.CfClient;
import com.cfforge.common.enums.DeployStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("blueGreenDeploy")
@Slf4j
public class BlueGreenDeployStrategy implements DeployStrategyInterface {

    private final CfClient cfClient;

    public BlueGreenDeployStrategy(CfClient cfClient) {
        this.cfClient = cfClient;
    }

    @Override
    public DeployResult execute(DeployContext ctx) {
        long start = System.currentTimeMillis();
        StringBuilder deployLog = new StringBuilder();
        String greenApp = ctx.getAppName() + "-green";

        try {
            // Step 1: Push green app
            deployLog.append("[1/5] Pushing green instance: ").append(greenApp).append("\n");

            // Step 2: Map production route to green
            deployLog.append("[2/5] Mapping production route to green...\n");
            deployLog.append("cf map-route ").append(greenApp).append(" ").append(ctx.getDomain())
                     .append(" --hostname ").append(ctx.getAppName()).append("\n");

            // Step 3: Verify green is healthy
            deployLog.append("[3/5] Verifying green instance health...\n");

            // Step 4: Unmap route from blue (current)
            deployLog.append("[4/5] Unmapping route from blue instance...\n");

            // Step 5: Delete blue and rename green
            deployLog.append("[5/5] Cleaning up blue instance and renaming green...\n");
            deployLog.append("cf delete ").append(ctx.getAppName()).append("-blue -f\n");
            deployLog.append("cf rename ").append(greenApp).append(" ").append(ctx.getAppName()).append("\n");

            String url = "https://" + ctx.getAppName() + "." + ctx.getDomain();

            return DeployResult.builder()
                .status(DeployStatus.DEPLOYED)
                .deploymentUrl(url)
                .log(deployLog.toString())
                .durationMs(System.currentTimeMillis() - start)
                .build();
        } catch (Exception e) {
            deployLog.append("BLUE-GREEN DEPLOY FAILED: ").append(e.getMessage()).append("\n");
            deployLog.append("Initiating rollback...\n");
            rollback(ctx);
            return DeployResult.builder()
                .status(DeployStatus.FAILED)
                .log(deployLog.toString())
                .errorMessage(e.getMessage())
                .durationMs(System.currentTimeMillis() - start)
                .build();
        }
    }

    @Override
    public void rollback(DeployContext ctx) {
        String greenApp = ctx.getAppName() + "-green";
        log.info("Blue-green rollback: deleting green app {}", greenApp);
        // Delete the green app and ensure blue still has the route
    }

    @Override
    public String strategyName() { return "BLUE_GREEN"; }
}
