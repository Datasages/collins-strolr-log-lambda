package com.wabtec.railwaynet.strolrloglambda.testutil.mixin;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public abstract class S3EventNotificationMixin {

    @JsonCreator
    public S3EventNotificationMixin(
        @JsonProperty("Records") List<S3EventNotification.S3EventNotificationRecord> records
    ) {}
}
