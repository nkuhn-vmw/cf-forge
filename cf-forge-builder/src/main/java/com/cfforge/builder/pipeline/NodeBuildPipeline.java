package com.cfforge.builder.pipeline;

import com.cfforge.builder.model.BuildContext;
import com.cfforge.builder.model.BuildResult;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.enums.Language;
import com.cfforge.common.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Component
@Slf4j
public class NodeBuildPipeline implements BuildPipeline {

    private final S3StorageService storageService;

    public NodeBuildPipeline(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public BuildResult execute(BuildContext context) {
        StringBuilder buildLog = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            buildLog.append("[1/4] Installing dependencies...\n");
            runCommand(context.getWorkDir(), "npm install", buildLog);

            buildLog.append("[2/4] Running tests...\n");
            runCommand(context.getWorkDir(), "npm test --if-present", buildLog);

            buildLog.append("[3/4] Building...\n");
            runCommand(context.getWorkDir(), "npm run build --if-present", buildLog);

            buildLog.append("[4/4] Packaging...\n");

            return BuildResult.builder()
                .status(BuildStatus.SUCCESS)
                .log(buildLog.toString())
                .durationMs(System.currentTimeMillis() - start)
                .build();
        } catch (Exception e) {
            buildLog.append("BUILD FAILED: ").append(e.getMessage()).append("\n");
            return BuildResult.builder()
                .status(BuildStatus.FAILED)
                .log(buildLog.toString())
                .errorMessage(e.getMessage())
                .durationMs(System.currentTimeMillis() - start)
                .build();
        }
    }

    @Override
    public Language supportedLanguage() {
        return Language.NODEJS;
    }

    private void runCommand(Path workDir, String command, StringBuilder log) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        pb.directory(workDir.toFile());
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
            throw new RuntimeException("Command failed: " + command);
        }
    }
}
