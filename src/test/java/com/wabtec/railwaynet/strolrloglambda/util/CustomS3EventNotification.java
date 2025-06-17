// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.util;

import java.util.List;

public class CustomS3EventNotification {
    public List<CustomS3EventNotificationRecord> Records;

    public CustomS3EventNotification() {
    }

    public CustomS3EventNotification(List<CustomS3EventNotificationRecord> records) {
        this.Records = records;
    }

    public List<CustomS3EventNotificationRecord> getRecords() {
        return Records;
    }
}

// This class is intentionally left empty as a placeholder for custom S3 event notification handling.