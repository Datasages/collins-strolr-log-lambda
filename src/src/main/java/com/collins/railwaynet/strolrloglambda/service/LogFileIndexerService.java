package com.collins.railwaynet.strolrloglambda.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.collins.railwaynet.strolrloglambda.entity.LogFile;
import com.collins.railwaynet.strolrloglambda.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;


public class LogFileIndexerService implements RequestHandler< S3Event, String> {
  private LambdaLogger logger;
    private static String PATH_DELIMITER = "/";
    private static String FILE_DELIMITER = "\\.";
      
  @Override
    public String handleRequest(S3Event event, Context ctx) {
      logger = ctx.getLogger();
        logger.log("Processing received File in S3: " + event);
        
    S3EventNotificationRecord record=event.getRecords().get(0);
    Pattern desiredFiles = Pattern.compile(".*log.gz");
    String srcBucket = record.getS3().getBucket().getName();
    String srcKey = record.getS3().getObject().getKey();
    Matcher matcher = desiredFiles.matcher(srcKey);
    if (!matcher.matches()) {
          logger.log("Skipping undesired file " + srcKey);
          return "Skipped";
    }
    //Split up file path on "/" in to array
    Pattern pathPattern = Pattern.compile(PATH_DELIMITER);
    String[] fullPathAsArray = pathPattern.split(srcKey);  
    int pathDepth = fullPathAsArray.length;
    //filename is last in array
    
    String logFileName = fullPathAsArray[pathDepth-1];   
    Pattern filePattern = Pattern.compile(FILE_DELIMITER);
    String[] fileNameAsArray = filePattern.split(logFileName);

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");    
    String mark = fileNameAsArray[1];
    int locoNumber = Integer.parseInt(fileNameAsArray[2]);   
    String device = fullPathAsArray[pathDepth-2];
    LocalDateTime endTime = LocalDateTime.parse(fileNameAsArray[3], dateFormatter);
    String fileURL = srcBucket + "/" + srcKey;  
    LogFile logfile = new LogFile(mark, locoNumber, device, endTime, fileURL);
    logger.log("Log file: " + logfile.toString());
    
    Transaction transaction = null;
    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
        transaction = session.beginTransaction();
        session.save(logfile);
        transaction.commit();
    } catch (Exception e) {
        if (transaction != null) {
            transaction.rollback();
        }
        e.printStackTrace();
    }                 
    return null;    
  }
}