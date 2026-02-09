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

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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

    private volatile S3Client s3Client;

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
        getClient().putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromBytes(content)
        );
    }

    public void putObject(String key, InputStream inputStream, long contentLength) {
        getClient().putObject(
            PutObjectRequest.builder().bucket(defaultBucket).key(key).build(),
            RequestBody.fromInputStream(inputStream, contentLength)
        );
    }

    public byte[] getObject(String key) {
        return getObject(defaultBucket, key);
    }

    public byte[] getObject(String bucket, String key) {
        return getClient().getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();
    }

    public void deleteObject(String key) {
        deleteObject(defaultBucket, key);
    }

    public void deleteObject(String bucket, String key) {
        getClient().deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
    }

    public List<String> listObjects(String prefix) {
        return listObjects(defaultBucket, prefix);
    }

    public List<String> listObjects(String bucket, String prefix) {
        ListObjectsV2Response response = getClient().listObjectsV2(
            ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build()
        );
        return response.contents().stream()
            .map(S3Object::key)
            .collect(Collectors.toList());
    }
}
