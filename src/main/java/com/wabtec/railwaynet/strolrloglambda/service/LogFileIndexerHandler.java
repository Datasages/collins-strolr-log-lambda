// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;
import com.wabtec.railwaynet.strolrloglambda.parser.LogFilePathParser;
import com.wabtec.railwaynet.strolrloglambda.parser.PathParser;
import com.wabtec.railwaynet.strolrloglambda.repository.JdbcLogFileRepository;
import com.wabtec.railwaynet.strolrloglambda.repository.LogFileRepository;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;





/**
 * Lambda handler that parses file metadata, saves it into Postgres, and optionally replicates the S3 object.
 */
public class LogFileIndexerHandler implements RequestHandler<S3Event, String> {

    private final PathParser parser;
    private final LogFileRepository repo;
    private final S3Service s3Service;
    private final boolean enableReplication;
    private final String replicationBucket;

    public boolean isEnableReplication() {
        return enableReplication;
    }

    public String getReplicationBucket() {
        return replicationBucket;
    }
    /**
     * Cold-start constructor: reads env vars and initializes real dependencies.
     */
    public LogFileIndexerHandler() {
    this.parser = new LogFilePathParser();
    this.repo = new JdbcLogFileRepository();

    Region region = Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));
    S3Client sdkS3 = S3Client.builder().region(region).build();
    this.s3Service = new S3ServiceImpl(sdkS3);

    String enable = System.getenv("ENABLE_REPLICATION");
    this.enableReplication = Boolean.parseBoolean(enable != null ? enable : "false");

    this.replicationBucket = System.getenv("REPLICATION_BUCKET_NAME");
    if (enableReplication && (replicationBucket == null || replicationBucket.isBlank())) {
        throw new IllegalStateException("ENABLE_REPLICATION is true but REPLICATION_BUCKET_NAME is missing");
    }

    
}


    /**
     * Constructor for unit tests or manual wiring.
     */
    public LogFileIndexerHandler(PathParser parser,
                                 LogFileRepository repo,
                                 S3Service s3Service,
                                 boolean enableReplication,
                                 String replicationBucket) {
        this.parser = parser;
        this.repo = repo;
        this.s3Service = s3Service;
        this.enableReplication = enableReplication;
        if (enableReplication && (replicationBucket == null || replicationBucket.isBlank())) {
            throw new IllegalStateException("REPLICATION_BUCKET_NAME must be defined when replication is enabled");
        }
        this.replicationBucket = replicationBucket;
    }

@Override
public String handleRequest(S3Event event, Context context) {
    S3EventNotificationRecord rec = event.getRecords().get(0);
    String bucket = rec.getS3().getBucket().getName();
    String key = rec.getS3().getObject().getKey();
    String decodedKey = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);

    try {
        @SuppressWarnings("unused")
        LogFile lf = parser.parse(decodedKey, bucket);
    } catch (Exception ex) {
        context.getLogger().log("Unparsable key: " + key + " — " + ex.getMessage());
        return "Skipped";
    }

    LogFile lf = parser.parse(decodedKey, bucket);
    if (lf == null) {
        context.getLogger().log("Skipping undesired file: " + key);
        return "Skipped";
    }

    repo.save(lf);

    @SuppressWarnings("unused")
    long size = s3Service.getFileSize(bucket, decodedKey);

    if (enableReplication) {
        s3Service.replicateFile(bucket, decodedKey, replicationBucket, extractFileName(decodedKey));
        context.getLogger().log("Replicated file to bucket: " + replicationBucket);
    }

    context.getLogger().log("Processed log file: " + lf);
    return "Success";
}


    private static String extractFileName(String key) {
        int idx = key.lastIndexOf('/');
        return (idx >= 0) ? key.substring(idx + 1) : key;
    }

    @SuppressWarnings("unused")
    private static String requireEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return val;
    }
}
