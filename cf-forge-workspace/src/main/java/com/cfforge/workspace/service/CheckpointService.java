package com.cfforge.workspace.service;

import com.cfforge.common.storage.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CheckpointService {

    private final S3StorageService s3;
    private final FileStorageService fileStorageService;
    private final int maxCheckpoints;

    public CheckpointService(S3StorageService s3, FileStorageService fileStorageService,
                              @Value("${cfforge.checkpoints.max:50}") int maxCheckpoints) {
        this.s3 = s3;
        this.fileStorageService = fileStorageService;
        this.maxCheckpoints = maxCheckpoints;
    }

    public CheckpointInfo createCheckpoint(UUID workspaceId, String description) {
        String checkpointId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String srcPrefix = "workspaces/" + workspaceId + "/";
        String destPrefix = "checkpoints/" + workspaceId + "/" + checkpointId + "/";

        // Copy all workspace files to checkpoint prefix
        List<String> keys = s3.listObjects("cf-forge-workspaces", srcPrefix);
        for (String key : keys) {
            byte[] content = s3.getObject("cf-forge-workspaces", key);
            String relativePath = key.replace(srcPrefix, "");
            s3.putObject("cf-forge-workspaces", destPrefix + "files/" + relativePath, content);
        }

        // Store metadata
        String metadata = String.format(
            "{\"id\":\"%s\",\"description\":\"%s\",\"timestamp\":\"%s\",\"fileCount\":%d}",
            checkpointId, description, timestamp, keys.size()
        );
        s3.putObject("cf-forge-workspaces", destPrefix + "metadata.json",
            metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Prune old checkpoints if needed
        pruneCheckpoints(workspaceId);

        log.info("Created checkpoint {} for workspace {} ({} files)", checkpointId, workspaceId, keys.size());
        return new CheckpointInfo(checkpointId, description, timestamp, keys.size());
    }

    public List<CheckpointInfo> listCheckpoints(UUID workspaceId) {
        String prefix = "checkpoints/" + workspaceId + "/";
        List<String> allKeys = s3.listObjects("cf-forge-workspaces", prefix);

        return allKeys.stream()
            .filter(k -> k.endsWith("metadata.json"))
            .map(k -> {
                byte[] content = s3.getObject("cf-forge-workspaces", k);
                String json = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                return parseCheckpointInfo(json);
            })
            .sorted(Comparator.comparing(CheckpointInfo::timestamp).reversed())
            .collect(Collectors.toList());
    }

    public void restoreCheckpoint(UUID workspaceId, String checkpointId) {
        String srcPrefix = "checkpoints/" + workspaceId + "/" + checkpointId + "/files/";
        String destPrefix = "workspaces/" + workspaceId + "/";

        // Clear current workspace
        List<String> currentKeys = s3.listObjects("cf-forge-workspaces", destPrefix);
        for (String key : currentKeys) {
            s3.deleteObject("cf-forge-workspaces", key);
        }

        // Copy checkpoint files to workspace
        List<String> checkpointKeys = s3.listObjects("cf-forge-workspaces", srcPrefix);
        for (String key : checkpointKeys) {
            byte[] content = s3.getObject("cf-forge-workspaces", key);
            String relativePath = key.replace(srcPrefix, "");
            s3.putObject("cf-forge-workspaces", destPrefix + relativePath, content);
        }

        log.info("Restored checkpoint {} for workspace {} ({} files)", checkpointId, workspaceId, checkpointKeys.size());
    }

    private void pruneCheckpoints(UUID workspaceId) {
        List<CheckpointInfo> checkpoints = listCheckpoints(workspaceId);
        if (checkpoints.size() > maxCheckpoints) {
            List<CheckpointInfo> toDelete = checkpoints.subList(maxCheckpoints, checkpoints.size());
            for (CheckpointInfo cp : toDelete) {
                String prefix = "checkpoints/" + workspaceId + "/" + cp.id() + "/";
                List<String> keys = s3.listObjects("cf-forge-workspaces", prefix);
                for (String key : keys) {
                    s3.deleteObject("cf-forge-workspaces", key);
                }
                log.info("Pruned old checkpoint {} for workspace {}", cp.id(), workspaceId);
            }
        }
    }

    private CheckpointInfo parseCheckpointInfo(String json) {
        // Simple JSON parsing without adding a dependency
        String id = extractJsonField(json, "id");
        String description = extractJsonField(json, "description");
        String timestamp = extractJsonField(json, "timestamp");
        int fileCount = Integer.parseInt(extractJsonField(json, "fileCount"));
        return new CheckpointInfo(id, description, timestamp, fileCount);
    }

    private String extractJsonField(String json, String field) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) return "";
        int colonIdx = json.indexOf(':', start);
        int valStart = json.indexOf('"', colonIdx);
        if (valStart < 0) {
            // Numeric value
            int numStart = colonIdx + 1;
            while (numStart < json.length() && !Character.isDigit(json.charAt(numStart))) numStart++;
            int numEnd = numStart;
            while (numEnd < json.length() && Character.isDigit(json.charAt(numEnd))) numEnd++;
            return json.substring(numStart, numEnd);
        }
        int valEnd = json.indexOf('"', valStart + 1);
        return json.substring(valStart + 1, valEnd);
    }

    public record CheckpointInfo(String id, String description, String timestamp, int fileCount) {}
}
