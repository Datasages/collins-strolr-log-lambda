// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3ServiceImplTest {

    private S3Client mockS3;
    private S3ServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockS3 = mock(S3Client.class);
        service = new S3ServiceImpl(mockS3, null, null); // No replication logic needed
    }


    @Test
    void getFileSize_returnsContentLength() {
        HeadObjectRequest expectedReq = HeadObjectRequest.builder()
            .bucket("my-bucket")
            .key("path/to/key")
            .build();

        when(mockS3.headObject(eq(expectedReq)))
            .thenReturn(HeadObjectResponse.builder()
                .contentLength(4567L)
                .build());

        long size = service.getFileSize("my-bucket", "path/to/key");

        assertEquals(4567L, size);
        verify(mockS3).headObject(eq(expectedReq));
    }

    @Test
    void replicateFile_performsCopy() {
        CopyObjectResponse mockResponse = CopyObjectResponse.builder()
            .copyObjectResult(CopyObjectResult.builder().eTag("etag").build())
            .build();

        when(mockS3.copyObject(any(CopyObjectRequest.class))).thenReturn(mockResponse);

        service.replicateFile(null, "src-bucket", "srcKey", "dest-bucket", "destKey");

        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(mockS3).copyObject(captor.capture());

        CopyObjectRequest actual = captor.getValue();
        assertEquals("src-bucket", actual.sourceBucket());
        assertEquals("srcKey", actual.sourceKey());
        assertEquals("dest-bucket", actual.destinationBucket());
        assertEquals("destKey", actual.destinationKey());
    }
    
    @Test
    void getFileSize_throwsRuntimeExceptionOnError() {
        when(mockS3.headObject(any(HeadObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("Not Found").build());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            service.getFileSize("bucket", "key"));

        assertTrue(ex.getMessage().contains("Error getting size"));
    }

    @Test
    void replicateFile_throwsRuntimeExceptionOnError() {
        when(mockS3.copyObject(any(CopyObjectRequest.class)))
            .thenThrow(S3Exception.builder().message("Access Denied").build());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            service.replicateFile(null, "a", "b", "c", "d"));
        assertTrue(ex.getMessage().contains("Error replicating S3 object"));
        assertEquals(S3Exception.class, ex.getCause().getClass());
    }
}
