package com.cfforge.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3StorageService {

    @Value("${s3.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${s3.access-key:minioadmin}")
    private String accessKey;

    @Value("${s3.secret-key:minioadmin}")
    private String secretKey;

    @Value("${s3.region:us-east-1}")
    private String region;

    @Value("${s3.bucket:cf-forge-artifacts}")
    private String defaultBucket;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(true)
            .build();
    }

    public void putObject(String key, byte[] content) {
        putObject(defaultBucket, key, content);
    }

    public void putObject(String bucket, String key, byte[] content) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromBytes(content)
        );
    }

    public void putObject(String key, InputStream inputStream, long contentLength) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(defaultBucket).key(key).build(),
            RequestBody.fromInputStream(inputStream, contentLength)
        );
    }

    public byte[] getObject(String key) {
        return getObject(defaultBucket, key);
    }

    public byte[] getObject(String bucket, String key) {
        return s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();
    }

    public void deleteObject(String key) {
        deleteObject(defaultBucket, key);
    }

    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucket).key(key).build()
        );
    }

    public List<String> listObjects(String prefix) {
        return listObjects(defaultBucket, prefix);
    }

    public List<String> listObjects(String bucket, String prefix) {
        ListObjectsV2Response response = s3Client.listObjectsV2(
            ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build()
        );
        return response.contents().stream()
            .map(S3Object::key)
            .collect(Collectors.toList());
    }
}
