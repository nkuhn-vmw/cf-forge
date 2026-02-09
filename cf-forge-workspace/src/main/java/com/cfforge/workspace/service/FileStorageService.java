package com.cfforge.workspace.service;

import com.cfforge.common.dto.FileEntry;
import com.cfforge.common.storage.S3StorageService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final S3StorageService s3;

    public FileStorageService(S3StorageService s3) {
        this.s3 = s3;
    }

    public String readFile(UUID workspaceId, String path) {
        String key = buildKey(workspaceId, path);
        byte[] content = s3.getObject("cf-forge-workspaces", key);
        return new String(content, StandardCharsets.UTF_8);
    }

    public void writeFile(UUID workspaceId, String path, String content) {
        String key = buildKey(workspaceId, path);
        s3.putObject("cf-forge-workspaces", key, content.getBytes(StandardCharsets.UTF_8));
    }

    public List<FileEntry> listFiles(UUID workspaceId, String dir) {
        String prefix = "workspaces/" + workspaceId + "/";
        if (dir != null && !dir.isEmpty()) {
            prefix += dir.endsWith("/") ? dir : dir + "/";
        }
        List<String> keys = s3.listObjects("cf-forge-workspaces", prefix);
        String finalPrefix = prefix;
        return keys.stream()
            .map(key -> {
                String relativePath = key.replace(finalPrefix, "");
                boolean isDir = relativePath.contains("/");
                String name = isDir ? relativePath.split("/")[0] : relativePath;
                return new FileEntry(name, relativePath, isDir, 0, null);
            })
            .distinct()
            .collect(Collectors.toList());
    }

    public void deleteFile(UUID workspaceId, String path) {
        String key = buildKey(workspaceId, path);
        s3.deleteObject("cf-forge-workspaces", key);
    }

    private String buildKey(UUID workspaceId, String path) {
        return "workspaces/" + workspaceId + "/" + path;
    }
}
