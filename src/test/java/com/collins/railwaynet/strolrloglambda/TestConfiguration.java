package com.collins.railwaynet.strolrloglambda;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = "com.collins.railwaynet.strolrloglambda",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com.collins.railwaynet.strolrloglambda.config.*"
    )
)
public class TestConfiguration {

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
                        "http://localhost:4566", "us-east-1"))
                .withCredentials(new com.amazonaws.auth.AWSStaticCredentialsProvider(
                        new com.amazonaws.auth.BasicAWSCredentials("test", "test")))
                .withPathStyleAccessEnabled(true)
                        .build();
    }
}