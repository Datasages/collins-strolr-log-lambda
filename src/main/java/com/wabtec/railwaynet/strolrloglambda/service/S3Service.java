package com.wabtec.railwaynet.strolrloglambda.service;

public interface S3Service {
    /**
     * Retrieve the size (in bytes) of the specified S3 object using a HEAD request.
     *
     * @param bucket the S3 bucket name
     * @param key    the path/key of the object
     * @return content length in bytes
     * @throws RuntimeException on AWS SDK or network errors
     */
    long getFileSize(String bucket, String key);

    /**
     * Replicate an S3 object from source to destination.
     *
     * @param srcBucket   source bucket name
     * @param srcKey      source object key
     * @param destBucket  destination bucket name
     * @param destKey     destination object key
     * @throws RuntimeException on AWS SDK or network errors
     */
    void replicateFile(String srcBucket, String srcKey, String destBucket, String destKey);
}
