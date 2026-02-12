package com.cfforge.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class S3StorageService {

    @Value("${s3.endpoint:}")
    private String endpoint;

    @Value("${s3.access-key:}")
    private String accessKey;

    @Value("${s3.secret-key:}")
    private String secretKey;

    @Value("${s3.region:us-east-1}")
    private String region;

    @Value("${s3.bucket:cf-forge-artifacts}")
    private String defaultBucket;

    @Value("${s3.fallback-dir:#{null}}")
    private String fallbackDir;

    private volatile S3Client s3Client;
    private volatile Boolean useFilesystem;

    private boolean isFilesystemMode() {
        if (useFilesystem == null) {
            synchronized (this) {
                if (useFilesystem == null) {
                    useFilesystem = (endpoint == null || endpoint.isBlank());
                    if (useFilesystem) {
                        String dir = (fallbackDir != null && !fallbackDir.isBlank()) ? fallbackDir : "/tmp/cf-forge-storage";
                        log.info("S3 not configured, using filesystem fallback at: {}", dir);
                    }
                }
            }
        }
        return useFilesystem;
    }

    private Path getFallbackPath(String bucket, String key) {
        String dir = (fallbackDir != null && !fallbackDir.isBlank()) ? fallbackDir : "/tmp/cf-forge-storage";
        return Paths.get(dir, bucket, key);
    }

    private S3Client getClient() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    if (endpoint == null || endpoint.isBlank()) {
                        throw new IllegalStateException("S3 storage is not configured (s3.endpoint is not set)");
                    }
                    this.s3Client = S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                        .forcePathStyle(true)
                        .build();
                    log.info("S3 client initialized: endpoint={}, bucket={}", endpoint, defaultBucket);
                }
            }
        }
        return s3Client;
    }

    public void putObject(String key, byte[] content) {
        putObject(defaultBucket, key, content);
    }

    public void putObject(String bucket, String key, byte[] content) {
        if (isFilesystemMode()) {
            try {
                Path path = getFallbackPath(bucket, key);
                Files.createDirectories(path.getParent());
                Files.write(path, content);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + key, e);
            }
            return;
        }
        getClient().putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromBytes(content)
        );
    }

    public void putObject(String key, InputStream inputStream, long contentLength) {
        if (isFilesystemMode()) {
            try {
                Path path = getFallbackPath(defaultBucket, key);
                Files.createDirectories(path.getParent());
                Files.copy(inputStream, path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file: " + key, e);
            }
            return;
        }
        getClient().putObject(
            PutObjectRequest.builder().bucket(defaultBucket).key(key).build(),
            RequestBody.fromInputStream(inputStream, contentLength)
        );
    }

    public byte[] getObject(String key) {
        return getObject(defaultBucket, key);
    }

    public byte[] getObject(String bucket, String key) {
        if (isFilesystemMode()) {
            try {
                Path path = getFallbackPath(bucket, key);
                if (!Files.exists(path)) {
                    throw new RuntimeException("File not found: " + key);
                }
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + key, e);
            }
        }
        return getClient().getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();
    }

    public void deleteObject(String key) {
        deleteObject(defaultBucket, key);
    }

    public void deleteObject(String bucket, String key) {
        if (isFilesystemMode()) {
            try {
                Path path = getFallbackPath(bucket, key);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file: " + key, e);
            }
            return;
        }
        getClient().deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
    }

    public List<String> listObjects(String prefix) {
        return listObjects(defaultBucket, prefix);
    }

    public List<String> listObjects(String bucket, String prefix) {
        if (isFilesystemMode()) {
            try {
                Path basePath = getFallbackPath(bucket, prefix);
                if (!Files.exists(basePath)) {
                    return Collections.emptyList();
                }
                Path bucketRoot = getFallbackPath(bucket, "");
                try (Stream<Path> walk = Files.walk(basePath)) {
                    return walk.filter(Files::isRegularFile)
                        .map(p -> bucketRoot.relativize(p).toString())
                        .collect(Collectors.toList());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list files: " + prefix, e);
            }
        }
        ListObjectsV2Response response = getClient().listObjectsV2(
            ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build()
        );
        return response.contents().stream()
            .map(S3Object::key)
            .collect(Collectors.toList());
    }
}
