package com.wabtec.railwaynet.strolrloglambda.testutil.mixin;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class S3EventNotificationRecordMixin {

    @JsonCreator
    public S3EventNotificationRecordMixin(
        @JsonProperty("eventVersion") String eventVersion,
        @JsonProperty("eventSource") String eventSource,
        @JsonProperty("awsRegion") String awsRegion,
        @JsonProperty("eventTime") String eventTime,
        @JsonProperty("eventName") String eventName,
        @JsonProperty(value = "userIdentity", required = false) UserIdentityEntity userIdentity,
        @JsonProperty(value = "requestParameters", required = false) RequestParametersEntity requestParameters,
        @JsonProperty(value = "responseElements", required = false) ResponseElementsEntity responseElements,
        @JsonProperty("s3") S3Entity s3
    ) {
        // MixIn constructor only for Jackson deserialization
    }
}
