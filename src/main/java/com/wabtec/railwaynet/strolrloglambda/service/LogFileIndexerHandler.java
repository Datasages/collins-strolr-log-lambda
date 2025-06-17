// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import org.slf4j.LoggerFactory;

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

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogFileIndexerHandler.class);

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

    // Choose ReplicationPathProcessor based on SCAC
    ReplicationPathProcessor processor = switch (System.getenv("SCAC")) {
        case "AMTK" -> new AmtkReplicationPathProcessor();
        default -> null;
    };

    this.s3Service = new S3ServiceImpl(
        sdkS3,
        System.getenv("SCAC"),
        processor
    );


    String enable = System.getenv("ENABLE_REPLICATION");
    this.enableReplication = Boolean.parseBoolean(enable != null ? enable : "false");
    LOGGER.debug(enableReplication ? "Replication is enabled" : "Replication is disabled");

    this.replicationBucket = System.getenv("REPLICATION_BUCKET_NAME");
    LOGGER.debug("Replication bucket: {}", replicationBucket);
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

    LogFile lf;
    try {
        lf = parser.parse(decodedKey, bucket);
        LOGGER.debug("Parsed key: {} from bucket: {}", decodedKey, bucket);
        if (lf == null) {
            LOGGER.warn("Skipping file because parser returned null for key: {} from bucket: {}", decodedKey, bucket);
            return "Skipped";
        }
    } catch (Exception ex) {
        LOGGER.warn("Skipping unparsable key: {} from bucket: {}", decodedKey, bucket);
        return "Skipped";
    }

    repo.save(lf);
    LOGGER.debug("Saved log file metadata to database: {}", lf);

    @SuppressWarnings("unused")
    long size = s3Service.getFileSize(bucket, decodedKey);

    if (enableReplication) {
        s3Service.replicateFile(lf, bucket, decodedKey, replicationBucket, extractFileName(decodedKey));

    }

    LOGGER.debug("Completed processing log file: {}", lf);
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
