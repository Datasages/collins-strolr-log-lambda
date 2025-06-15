// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import java.util.Objects;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * AWS SDK v2-based S3 service for file metadata and replication.
 */
public class S3ServiceImpl implements S3Service {

    private final S3Client s3;

    public S3ServiceImpl(S3Client s3Client) {
        this.s3 = Objects.requireNonNull(s3Client, "s3Client");
    }

    /**
     * Retrieves size of the S3 object via a HEAD request.
     */
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


    /**
     * Copies an object (≤5GB) from source to destination in S3.
     */
    @Override
    public void replicateFile(String srcBucket, String srcKey, String destBucket, String destKey) {
        try {
            s3.copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(srcBucket)
                    .sourceKey(srcKey)
                    .destinationBucket(destBucket)
                    .destinationKey(destKey)
                    .build()
            );
        } catch (S3Exception e) {
            throw new RuntimeException("Error replicating S3 object from " +
                srcBucket + "/" + srcKey + " to " + destBucket + "/" + destKey, e);
        }
    }
}
    
// This code implements the S3Service interface using AWS SDK v2, providing methods to get file size and replicate files in S3.