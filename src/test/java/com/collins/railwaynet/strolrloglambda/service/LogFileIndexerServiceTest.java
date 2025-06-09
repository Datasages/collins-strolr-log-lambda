package com.collins.railwaynet.strolrloglambda.service;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.collins.railwaynet.strolrloglambda.entity.LogFile;
import com.collins.railwaynet.strolrloglambda.parser.PathParser;
import com.collins.railwaynet.strolrloglambda.repository.LogFileRepository;

@ExtendWith(MockitoExtension.class)
class LogFileIndexerServiceTest {

    @Mock
    private PathParser pathParser;

    @Mock
    private LogFileRepository logFileRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private LogFileIndexerService logFileIndexerService;

    private S3Event s3Event;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        // Set replicationBucket via reflection
        try {
            java.lang.reflect.Field replicationBucketField = LogFileIndexerService.class.getDeclaredField("replicationBucket");
            replicationBucketField.setAccessible(true);
            replicationBucketField.set(logFileIndexerService, "railwaynet.mdm.amtk");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set replicationBucket field: " + e.getMessage());
        }

        S3EventNotification.S3EventNotificationRecord record = mock(S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3BucketEntity bucket = mock(S3EventNotification.S3BucketEntity.class);
        S3EventNotification.S3ObjectEntity object = mock(S3EventNotification.S3ObjectEntity.class);

        when(record.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(bucket);
        when(s3Entity.getObject()).thenReturn(object);
        when(bucket.getName()).thenReturn("amtk-mdmlogs");
        when(object.getKey()).thenReturn("amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz");

        s3Event = new S3Event(Collections.singletonList(record));
    }

    @Test
    void testHandleRequest_SuccessfulProcessing() {
        LogFile logFile = new LogFile("AMTK", 10, "CPU-3", LocalDateTime.of(2025, 6, 5, 4, 21, 30),
                "https://s3.amazonaws.com/amtk-mdmlogs/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz");
        when(pathParser.parse(anyString(), anyString())).thenReturn(logFile);
        when(s3Service.getFileSize(anyString(), anyString())).thenReturn(1024L);

        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        String result = logFileIndexerService.handleRequest(s3Event, context);

        verify(pathParser).parse(eq("amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz"), eq("amtk-mdmlogs"));
        verify(logFileRepository).save(logFile);
        verify(s3Service).getFileSize(eq("amtk-mdmlogs"), anyString());
        verify(s3Service).storeDataWarehouseMetadata(eq("amtk-mdmlogs"), eq(logFile), eq("app.AMTK.10.20250605042130.log.gz"), eq(1024L));
        verify(s3Service).replicateFile(eq("amtk-mdmlogs"), anyString(), eq("railwaynet.mdm.amtk"), eq(logFile), eq("app.AMTK.10.20250605042130.log.gz"));
        verify(logger).log(anyString());
        assertEquals("Success", result);
    }

    @Test
    void testHandleRequest_SkippedFile() {
        when(pathParser.parse(anyString(), anyString())).thenReturn(null);

        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        String result = logFileIndexerService.handleRequest(s3Event, context);

        verify(pathParser).parse(anyString(), anyString());
        verifyNoInteractions(logFileRepository, s3Service);
        verify(logger).log(anyString());
        assertEquals("Skipped", result);
    }

    @Test
    void testHandleRequest_CustomReplicationBucket() {
        // Set custom replication bucket via reflection
        try {
            java.lang.reflect.Field replicationBucketField = LogFileIndexerService.class.getDeclaredField("replicationBucket");
            replicationBucketField.setAccessible(true);
            replicationBucketField.set(logFileIndexerService, "ptc-p-logs");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set replicationBucket field: " + e.getMessage());
        }

        LogFile logFile = new LogFile("AMTK", 10, "CPU-3", LocalDateTime.of(2025, 6, 5, 4, 21, 30),
                "https://s3.amazonaws.com/amtk-mdmlogs/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz");
        when(pathParser.parse(anyString(), anyString())).thenReturn(logFile);
        when(s3Service.getFileSize(anyString(), anyString())).thenReturn(1024L);

        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        String result = logFileIndexerService.handleRequest(s3Event, context);

        verify(s3Service).replicateFile(eq("amtk-mdmlogs"), anyString(), eq("ptc-p-logs"), eq(logFile), eq("app.AMTK.10.20250605042130.log.gz"));
        verify(logger).log(anyString());
        assertEquals("Success", result);
    }
}