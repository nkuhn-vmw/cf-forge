package com.cfforge.builder.pipeline;

import com.cfforge.builder.model.BuildContext;
import com.cfforge.builder.model.BuildResult;
import com.cfforge.common.enums.BuildStatus;
import com.cfforge.common.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class PythonBuildPipeline implements BuildPipeline {

    @Override
    public BuildResult execute(BuildContext context) {
        StringBuilder buildLog = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            buildLog.append("[1/4] Creating virtual environment...\n");
            runCommand(context.getWorkDir(), "python3 -m venv .venv", buildLog);

            buildLog.append("[2/4] Installing dependencies...\n");
            if (Files.exists(context.getWorkDir().resolve("requirements.txt"))) {
                runCommand(context.getWorkDir(), ".venv/bin/pip install -r requirements.txt", buildLog);
            } else if (Files.exists(context.getWorkDir().resolve("pyproject.toml"))) {
                runCommand(context.getWorkDir(), ".venv/bin/pip install .", buildLog);
            }

            buildLog.append("[3/4] Running tests...\n");
            if (Files.exists(context.getWorkDir().resolve("tests")) ||
                Files.exists(context.getWorkDir().resolve("test"))) {
                runCommand(context.getWorkDir(), ".venv/bin/python -m pytest --tb=short", buildLog);
            } else {
                buildLog.append("  No tests directory found, skipping.\n");
            }

            buildLog.append("[4/4] Packaging...\n");
            // Ensure Procfile exists
            if (!Files.exists(context.getWorkDir().resolve("Procfile"))) {
                buildLog.append("  Warning: No Procfile found. CF needs a Procfile for Python apps.\n");
            }

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
        return Language.PYTHON;
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
