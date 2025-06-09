package com.collins.railwaynet.strolrloglambda.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.collins.railwaynet.strolrloglambda.entity.LogFile;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    @Mock
    private AmazonS3 s3Client;

    private S3ServiceImpl s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3ServiceImpl(s3Client);
    }

    @Test
    void testReplicateFile_ValidReplication() {
        LogFile logFile = new LogFile("AMTK", 10, "CPU-3", LocalDateTime.of(2025, 6, 5, 4, 21, 30), "s3://test");
        String srcBucket = "mdm.amtk.source"; // Updated to match condition
        String srcKey = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
        String destBucket = "ptc-p-logs";
        String logFileName = "app.AMTK.10.20250605042130.log.gz";

        // Enable replication using reflection with try-catch
        try {
            java.lang.reflect.Field enableReplicationField = s3Service.getClass().getDeclaredField("enableReplication");
            enableReplicationField.setAccessible(true);
            enableReplicationField.set(s3Service, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set enableReplication field: " + e.getMessage());
        }

        s3Service.replicateFile(srcBucket, srcKey, destBucket, logFile, logFileName);

        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(captor.capture());

        CopyObjectRequest request = captor.getValue();
        assertEquals(srcBucket, request.getSourceBucketName());
        assertEquals(srcKey, request.getSourceKey());
        assertEquals(destBucket, request.getDestinationBucketName());
        assertEquals("Sorted_Logs_for_05_JUN_2025/amtk.10.05_JUN_2025/CPU-3/app.AMTK.10.20250605042130.log.gz", request.getDestinationKey());
    }

    @Test
    void testReplicateFile_SkipNonAmtkBucket() {
        LogFile logFile = new LogFile("AMTK", 10, "CPU-3", LocalDateTime.now(), "s3://test");
        String srcBucket = "other-bucket";
        String srcKey = "test.log.gz";
        String destBucket = "ptc-p-logs";

        s3Service.replicateFile(srcBucket, srcKey, destBucket, logFile, "test.log.gz");

        verifyNoInteractions(s3Client);
    }

    @Test
    void testGetFileSize() {
        String bucket = "amtk-mdmlogs";
        String key = "test.log.gz";
        com.amazonaws.services.s3.model.ObjectMetadata metadata = new com.amazonaws.services.s3.model.ObjectMetadata();
        metadata.setContentLength(1024L);
        when(s3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);

        long size = s3Service.getFileSize(bucket, key);

        assertEquals(1024L, size);
    }
}