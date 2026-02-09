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
import java.util.stream.Collectors;

@Component
@Slf4j
public class JavaBuildPipeline implements BuildPipeline {

    private final S3StorageService storageService;

    public JavaBuildPipeline(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public BuildResult execute(BuildContext context) {
        StringBuilder buildLog = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            // Step 1: Download project files from S3
            buildLog.append("[1/6] Pulling project files from storage...\n");
            downloadProjectFiles(context);

            // Step 2: Resolve dependencies
            buildLog.append("[2/6] Resolving Maven dependencies...\n");
            runMavenCommand(context.getWorkDir(), "dependency:resolve", buildLog);

            // Step 3: Compile
            buildLog.append("[3/6] Compiling...\n");
            runMavenCommand(context.getWorkDir(), "compile", buildLog);

            // Step 4: Run tests
            buildLog.append("[4/6] Running tests...\n");
            runMavenCommand(context.getWorkDir(), "test", buildLog);

            // Step 5: Package
            buildLog.append("[5/6] Packaging...\n");
            runMavenCommand(context.getWorkDir(), "package -DskipTests", buildLog);

            // Step 6: Upload artifact
            buildLog.append("[6/6] Uploading artifact...\n");
            String artifactPath = uploadArtifact(context);

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
        return Language.JAVA;
    }

    private void downloadProjectFiles(BuildContext context) {
        var keys = storageService.listObjects("workspaces/" + context.getProjectId() + "/");
        for (String key : keys) {
            byte[] content = storageService.getObject(key);
            String relativePath = key.replace("workspaces/" + context.getProjectId() + "/", "");
            Path target = context.getWorkDir().resolve(relativePath);
            try {
                Files.createDirectories(target.getParent());
                Files.write(target, content);
            } catch (Exception e) {
                log.warn("Failed to download file: {}", key, e);
            }
        }
    }

    private void runMavenCommand(Path workDir, String goals, StringBuilder log) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("mvn", goals.split(" ")[0]);
        if (goals.contains(" ")) {
            pb.command().addAll(java.util.List.of(goals.split(" ")).subList(1, goals.split(" ").length));
        }
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
            throw new RuntimeException("Maven command failed with exit code: " + exitCode);
        }
    }

    private String uploadArtifact(BuildContext context) throws Exception {
        Path targetDir = context.getWorkDir().resolve("target");
        if (Files.exists(targetDir)) {
            var jars = Files.list(targetDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());
            if (!jars.isEmpty()) {
                byte[] artifact = Files.readAllBytes(jars.get(0));
                String key = "artifacts/" + context.getProjectId() + "/" + context.getBuildId() + ".jar";
                storageService.putObject(key, artifact);
                return key;
            }
        }
        return null;
    }
}
