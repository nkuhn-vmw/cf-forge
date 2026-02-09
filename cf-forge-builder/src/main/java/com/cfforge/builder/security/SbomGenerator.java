package com.cfforge.builder.security;

import com.cfforge.common.enums.Language;
import com.cfforge.common.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class SbomGenerator {

    private final S3StorageService storageService;

    public SbomGenerator(S3StorageService storageService) {
        this.storageService = storageService;
    }

    public String generate(Path workDir, Language language, UUID projectId, UUID buildId) {
        try {
            Path sbomFile = switch (language) {
                case JAVA -> generateMavenSbom(workDir);
                case NODEJS -> generateNpmSbom(workDir);
                case PYTHON -> generatePipSbom(workDir);
                case GO -> generateGoSbom(workDir);
                default -> null;
            };

            if (sbomFile != null && Files.exists(sbomFile)) {
                byte[] sbomContent = Files.readAllBytes(sbomFile);
                String key = "sboms/" + projectId + "/" + buildId + "/sbom.json";
                storageService.putObject(key, sbomContent);
                log.info("SBOM generated and uploaded: {} ({} bytes)", key, sbomContent.length);
                return key;
            }

            log.warn("SBOM generation produced no output for language: {}", language);
            return null;
        } catch (Exception e) {
            log.warn("SBOM generation failed: {}", e.getMessage());
            return null;
        }
    }

    private Path generateMavenSbom(Path workDir) throws Exception {
        // Use CycloneDX Maven plugin
        ProcessBuilder pb = new ProcessBuilder(
            "mvn", "org.cyclonedx:cyclonedx-maven-plugin:makeBom",
            "-DoutputFormat=json", "-DoutputName=sbom"
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        runProcess(pb);

        Path sbomPath = workDir.resolve("target/sbom.json");
        return Files.exists(sbomPath) ? sbomPath : null;
    }

    private Path generateNpmSbom(Path workDir) throws Exception {
        // Use npm sbom or cyclonedx-npm
        Path sbomPath = workDir.resolve("sbom.json");
        ProcessBuilder pb = new ProcessBuilder(
            "npx", "@cyclonedx/cyclonedx-npm", "--output-file", sbomPath.toString()
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        runProcess(pb);

        return Files.exists(sbomPath) ? sbomPath : null;
    }

    private Path generatePipSbom(Path workDir) throws Exception {
        Path sbomPath = workDir.resolve("sbom.json");
        ProcessBuilder pb = new ProcessBuilder(
            "cyclonedx-py", "-r", "--format", "json", "-o", sbomPath.toString()
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        runProcess(pb);

        return Files.exists(sbomPath) ? sbomPath : null;
    }

    private Path generateGoSbom(Path workDir) throws Exception {
        Path sbomPath = workDir.resolve("sbom.json");
        ProcessBuilder pb = new ProcessBuilder(
            "cyclonedx-gomod", "app", "-json", "-output", sbomPath.toString()
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        runProcess(pb);

        return Files.exists(sbomPath) ? sbomPath : null;
    }

    private void runProcess(ProcessBuilder pb) throws Exception {
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // consume output
            }
        }
        process.waitFor();
    }
}
