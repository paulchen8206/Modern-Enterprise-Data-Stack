package com.modern.enterprise.workflowapi.service;

import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {
  private final AppConfigProperties.Minio cfg;
  private final S3Client s3;

  public StorageService(AppConfigProperties props) {
    this.cfg = props.getMinio();
    // MinIO requires path-style S3 addressing in most local setups.
    this.s3 = S3Client.builder()
        .endpointOverride(URI.create("http://" + cfg.getEndpoint()))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
        .region(Region.US_EAST_1)
        .forcePathStyle(true)
        .build();
  }

  public void uploadRaw(String key, String json) {
    // Ensure destination bucket exists before write to keep API idempotent for first run.
    ensureBucket(cfg.getBucketRaw());
    s3.putObject(PutObjectRequest.builder().bucket(cfg.getBucketRaw()).key(key).build(),
        RequestBody.fromString(json, StandardCharsets.UTF_8));
  }

  public boolean canReachMinio() {
    try {
      ensureBucket(cfg.getBucketRaw());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private void ensureBucket(String bucket) {
    try {
      s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (NoSuchBucketException ex) {
      s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    } catch (Exception ex) {
      // For local MinIO, headBucket can throw generic errors for missing bucket.
      s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }
  }
}
