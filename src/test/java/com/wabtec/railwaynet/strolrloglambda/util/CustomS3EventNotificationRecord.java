// Copyright Wabtec Inc. 2025. All rights reserved
package com.wabtec.railwaynet.strolrloglambda.util;

public class CustomS3EventNotificationRecord {
    public String eventVersion;
    public String eventSource;
    public String awsRegion;
    public String eventTime;
    public String eventName;
    public CustomS3Entity s3;

    public CustomS3EventNotificationRecord() {}

    public CustomS3EventNotificationRecord(
        String eventVersion,
        String eventSource,
        String awsRegion,
        String eventTime,
        String eventName,
        CustomS3Entity s3
    ) {
        this.eventVersion = eventVersion;
        this.eventSource = eventSource;
        this.awsRegion = awsRegion;
        this.eventTime = eventTime;
        this.eventName = eventName;
        this.s3 = s3;
    }
}
