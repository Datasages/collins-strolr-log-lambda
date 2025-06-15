package com.wabtec.railwaynet.strolrloglambda.util;

public class CustomS3Entity {
    public CustomS3Bucket bucket;
    public CustomS3Object object;

    public CustomS3Entity() {}

    public CustomS3Entity(CustomS3Bucket bucket, CustomS3Object object) {
        this.bucket = bucket;
        this.object = object;
    }
}
