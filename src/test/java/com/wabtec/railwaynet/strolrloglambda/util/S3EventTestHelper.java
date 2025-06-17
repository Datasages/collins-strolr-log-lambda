// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.util;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.databind.ObjectMapper;

public class S3EventTestHelper {

    public static S3Event loadEvent(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Convert to custom local POJOs
        CustomS3EventNotification wrapper = mapper.readValue(json, CustomS3EventNotification.class);

        // Step 2: Map to AWS SDK classes
        List<S3EventNotification.S3EventNotificationRecord> records = wrapper.Records.stream()
            .map(S3EventTestHelper::toSdkRecord)
            .collect(Collectors.toList());

        // Step 3: Create actual S3Event
        return new S3Event(records);
    }

    public static S3Event fromKey(String bucket, String key) {
        S3EventNotification.S3ObjectEntity object = new S3EventNotification.S3ObjectEntity(key, 123L, null, null, null);
        S3EventNotification.S3BucketEntity bucketEntity = new S3EventNotification.S3BucketEntity(bucket, null, null);
        S3EventNotification.S3Entity s3Entity = new S3EventNotification.S3Entity(null, bucketEntity, object, null);

        S3EventNotification.S3EventNotificationRecord record =
            new S3EventNotification.S3EventNotificationRecord(
                "2.1",                 // eventVersion
                "aws:s3",              // eventSource
                "us-east-1",           // awsRegion
                "2025-06-14T17:00:00.000Z", // eventTime
                "ObjectCreated:Put",   // eventName
                null,                  // requestParameters
                null,                  // responseElements
                s3Entity,              // s3
                null                   // userIdentity
            );

        return new S3Event(List.of(record));
    }


    private static S3EventNotification.S3EventNotificationRecord toSdkRecord(CustomS3EventNotificationRecord r) {
        return new S3EventNotification.S3EventNotificationRecord(
            r.eventVersion,
            r.eventSource,
            r.awsRegion,
            r.eventTime,
            r.eventName,
            null, // requestParameters
            null, // responseElements
            new S3EventNotification.S3Entity(
                "1.0", // schemaVersion
                new S3EventNotification.S3BucketEntity(
                    r.s3.bucket.name,
                    null, // arn
                    null  // ownerIdentity
                ),
                new S3EventNotification.S3ObjectEntity(
                    r.s3.object.key,
                    null, // size
                    null, // eTag
                    null, // versionId
                    null  // sequencer
                ),
                null // configurationId
            ),
            null // userIdentity
        );
    }
}
