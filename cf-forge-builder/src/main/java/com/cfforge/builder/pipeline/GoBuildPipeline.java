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
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class GoBuildPipeline implements BuildPipeline {

    private final S3StorageService storageService;

    public GoBuildPipeline(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public BuildResult execute(BuildContext context) {
        StringBuilder buildLog = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            buildLog.append("[1/5] Downloading dependencies...\n");
            runCommand(context.getWorkDir(), "go mod download", buildLog);

            buildLog.append("[2/5] Verifying modules...\n");
            runCommand(context.getWorkDir(), "go mod verify", buildLog);

            buildLog.append("[3/5] Running tests...\n");
            runCommand(context.getWorkDir(), "go test ./...", buildLog);

            buildLog.append("[4/5] Building binary...\n");
            runCommand(context.getWorkDir(), "CGO_ENABLED=0 GOOS=linux go build -o app .", buildLog);

            buildLog.append("[5/5] Uploading artifact...\n");
            Path binary = context.getWorkDir().resolve("app");
            String artifactPath = null;
            if (Files.exists(binary)) {
                byte[] artifact = Files.readAllBytes(binary);
                String key = "artifacts/" + context.getProjectId() + "/" + context.getBuildId() + "/app";
                storageService.putObject(key, artifact);
                artifactPath = key;
            }

            return BuildResult.builder()
                .status(BuildStatus.SUCCESS)
                .log(buildLog.toString())
                .artifactPath(artifactPath)
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
        return Language.GO;
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
