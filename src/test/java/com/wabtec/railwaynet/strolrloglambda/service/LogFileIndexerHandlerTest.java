// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;
import com.wabtec.railwaynet.strolrloglambda.parser.PathParser;
import com.wabtec.railwaynet.strolrloglambda.repository.LogFileRepository;
import com.wabtec.railwaynet.strolrloglambda.util.S3EventTestHelper;

class LogFileIndexerHandlerTest {

    private PathParser mockParser;
    private LogFileRepository mockRepo;
    private S3Service mockS3Service;
    private Context mockContext;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        mockParser = mock(PathParser.class);
        mockRepo = mock(LogFileRepository.class);
        mockS3Service = mock(S3Service.class);
        mockContext = mock(Context.class);
        var mockLogger = mock(com.amazonaws.services.lambda.runtime.LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
         System.setProperty("POWERTOOLS_LOG_LEVEL", "DEBUG"); 
    }
@AfterEach
    @SuppressWarnings("unused")
    void tearDown() {
        System.clearProperty("POWERTOOLS_LOG_LEVEL");
    }


    @Test
    void handle_skipsWhenParserReturnsNull() throws Exception {
        when(mockParser.parse(any(), any())).thenReturn(null);
        var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, false, "unused-bucket");

        S3Event event = loadEvent();
        String result = handler.handleRequest(event, mockContext);

        assertEquals("Skipped", result);
        verifyNoInteractions(mockRepo, mockS3Service);
    }

@Test
void handle_savesWithoutReplicationWhenDisabled() throws Exception {
    String expectedBucket = "test-bucket";
    String expectedKey = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";

    LogFile lf = new LogFile("MK", 99, "CPU-1",
        LocalDateTime.of(2025, 1, 1, 0, 0), "url");

    when(mockParser.parse(expectedKey, expectedBucket)).thenReturn(lf);
    when(mockS3Service.getFileSize(expectedBucket, expectedKey)).thenReturn(123L);

    var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, false, "dest-bucket");

    S3Event event = loadEvent();
    String result = handler.handleRequest(event, mockContext);

    assertEquals("Success", result);
    verify(mockParser, atLeastOnce()).parse(expectedKey, expectedBucket);
    verify(mockRepo).save(lf);
    verify(mockS3Service).getFileSize(expectedBucket, expectedKey);
    verify(mockS3Service, never()).replicateFile(any(), any(), any(), any());
}


    @Test
    void handle_replicatesWhenEnabled() throws Exception {
        String expectedBucket = "test-bucket";
        String expectedKey = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
        String expectedFileName = "app.AMTK.10.20250605042130.log.gz";

        LogFile lf = new LogFile("MK", 99, "CPU-1",
            LocalDateTime.of(2025, 1, 1, 0, 0), "url");

        when(mockParser.parse(expectedKey, expectedBucket)).thenReturn(lf);
        when(mockS3Service.getFileSize(expectedBucket, expectedKey)).thenReturn(123L);

        var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, true, "dest-bucket");

        S3Event event = loadEvent();
        String result = handler.handleRequest(event, mockContext);

        assertEquals("Success", result);
        verify(mockParser, atLeastOnce()).parse(expectedKey, expectedBucket);
        verify(mockRepo).save(lf);
        verify(mockS3Service).getFileSize(expectedBucket, expectedKey);
        verify(mockS3Service).replicateFile(expectedBucket, expectedKey, "dest-bucket", expectedFileName);
    }

@Test
void handle_processesRealisticMdmPath() throws Exception {
    String key = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
    LogFile expected = new LogFile("AMTK", 10, "CPU-3", 
        LocalDateTime.of(2025, 6, 5, 4, 21, 30), 
        "https://s3.amazonaws.com/test-bucket/" + key);

    when(mockParser.parse(key, "test-bucket")).thenReturn(expected);
    when(mockS3Service.getFileSize("test-bucket", key)).thenReturn(100L);

    var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, true, "dest-bucket");

    S3Event event = loadEvent();
    String result = handler.handleRequest(event, mockContext);

    assertEquals("Success", result);
    verify(mockRepo).save(expected);
    verify(mockS3Service).replicateFile(
        "test-bucket", 
        key, 
        "dest-bucket", 
        "app.AMTK.10.20250605042130.log.gz");
}


    @Test
    void constructor_noReplication_doesNotRequireBucket() {
        var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, false, null);
        assertFalse(handler.isEnableReplication());
        assertNull(handler.getReplicationBucket());
    }

    @Test
    void constructor_replicationEnabled_withBucket_succeeds() {
        var handler = new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, true, "my-bucket");
        assertTrue(handler.isEnableReplication());
        assertEquals("my-bucket", handler.getReplicationBucket());
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    void constructor_replicationEnabled_withoutBucket_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            new LogFileIndexerHandler(mockParser, mockRepo, mockS3Service, true, null);
        });
        assertTrue(ex.getMessage().contains("REPLICATION_BUCKET_NAME"));
    }



    private static S3Event loadEvent() throws IOException {
        String json = Files.readString(Path.of("src/test/resources/s3-event.json"));
        return S3EventTestHelper.loadEvent(json);
    }
}
