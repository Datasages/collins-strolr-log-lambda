// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * AWS SDK v2-based S3 service for file metadata and replication.
 */
public class S3ServiceImpl implements S3Service {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(S3ServiceImpl.class);

    private final S3Client s3;
    private final String scac;
    /** Resolved at construction: the processor for this SCAC, or null if none applies. */
    private final ReplicationPathProcessor processor;

    public S3ServiceImpl(S3Client s3Client, String scac, ReplicationPathProcessor processor) {
        this.s3 = Objects.requireNonNull(s3Client, "s3Client");
        this.scac = scac;
        // A processor only applies when we have both a SCAC and a processor for it.
        this.processor = (scac != null) ? processor : null;
    }

    @Override
    public long getFileSize(String bucket, String key) {
        try {
            HeadObjectRequest req = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            HeadObjectResponse resp = s3.headObject(req);
            return resp.contentLength();
        } catch (S3Exception e) {
            throw new RuntimeException("Error getting size for S3://" + bucket + "/" + key, e);
        }
    }

    @Override
    public void replicateFile(LogFile lf, String srcBucket, String srcKey, String destBucket, String destKey) {
        // The processor (if any) was resolved at construction — no System.getenv here.
        if (processor != null) {
            destKey = processor.getReplicationPath(lf) + destKey;
        } else {
            LOGGER.warn("No replication path processor found for SCAC: {}", scac);
        }

        try {
            s3.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(srcBucket)
                    .sourceKey(srcKey)
                    .destinationBucket(destBucket)
                    .destinationKey(destKey)
                    .build()
            );
            LOGGER.debug("Destination Bucket: {}", destBucket);
            LOGGER.debug("Replicated S3 object from {}/{} to {}/{}", srcBucket, srcKey, destBucket, destKey);
        } catch (S3Exception e) {
            LOGGER.warn("Failed to replicate S3 object from {}/{} to {}/{}", srcBucket, srcKey, destBucket, destKey, e);
            throw new RuntimeException("Error replicating S3 object from " +
                srcBucket + "/" + srcKey + " to " + destBucket + "/" + destKey, e);
        }
    }
}
