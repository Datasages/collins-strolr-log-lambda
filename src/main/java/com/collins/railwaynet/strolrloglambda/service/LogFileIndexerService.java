// Lambda Handler
package com.collins.railwaynet.strolrloglambda.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.collins.railwaynet.strolrloglambda.entity.LogFile;
import com.collins.railwaynet.strolrloglambda.parser.PathParser;
import com.collins.railwaynet.strolrloglambda.repository.LogFileRepository;

@Component
public class LogFileIndexerService implements RequestHandler<S3Event, String> {

    private final PathParser pathParser;
    private final LogFileRepository logFileRepository;
    private final S3Service s3Service;

    @Value("${aws.amtk.replication.bucket:railwaynet.mdm.amtk}")
    private String replicationBucket;

    public LogFileIndexerService(PathParser pathParser, LogFileRepository logFileRepository, S3Service s3Service) {
        this.pathParser = pathParser;
        this.logFileRepository = logFileRepository;
        this.s3Service = s3Service;
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        S3EventNotificationRecord record = event.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getKey();
        String decodedSrcKey = URLDecoder.decode(srcKey, StandardCharsets.UTF_8);

        LogFile logFile = pathParser.parse(decodedSrcKey, srcBucket);
        if (logFile == null) {
            context.getLogger().log("Skipping undesired file: " + srcKey);
            return "Skipped";
        }

        logFileRepository.save(logFile);

        long fileSize = s3Service.getFileSize(srcBucket, decodedSrcKey);
        s3Service.storeDataWarehouseMetadata(srcBucket, logFile, srcKey.substring(srcKey.lastIndexOf("/") + 1), fileSize);
        s3Service.replicateFile(srcBucket, decodedSrcKey, replicationBucket, logFile, srcKey.substring(srcKey.lastIndexOf("/") + 1));

        context.getLogger().log("Processed log file: " + logFile.toString());
        return "Success";
    }
}