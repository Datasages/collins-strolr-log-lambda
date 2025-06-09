// S3 Service Interface
package com.collins.railwaynet.strolrloglambda.service;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;

public interface S3Service {
    long getFileSize(String bucket, String key);
    void replicateFile(String srcBucket, String srcKey, String destBucket, LogFile logFile, String logFileName);
    void storeDataWarehouseMetadata(String bucket, LogFile logFile, String fileName, long fileSize);
}
