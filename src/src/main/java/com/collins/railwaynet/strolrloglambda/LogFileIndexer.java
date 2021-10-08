package com.collins.railwaynet.strolrloglambda;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class LogFileIndexer implements RequestHandler< S3Event, String> {
  private LambdaLogger logger;
    private static String PATH_DELIMITER = "/";
    private static String FILE_DELIMITER = ".";
    private static String DB_URL = "m-ris-postgres-db.cjtxx3u946t0.us-east-1.rds.amazonaws.com";
    private static String DB_NAME = "strolr-logfiledb";
    private static String DB_TABLE = "logfileindex";
    private static String DB_USER = "strolr_logfiledb_writer";
    private static String DB_PW = "ygZ7XuENjsBHwVyy";
    
  
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
    String fileURL = srcBucket + "/" + srcKey;
    
    Pattern pathPattern = Pattern.compile(PATH_DELIMITER);
    String[] fullPathAsArray = pathPattern.split(srcKey);
    int pathDepth = fullPathAsArray.length;
    String device = fullPathAsArray[pathDepth-2];
    String logFileName = fullPathAsArray[pathDepth-1];
    
    Pattern filePattern = Pattern.compile(FILE_DELIMITER);
    String[] fileNameAsArray = filePattern.split(logFileName);
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    String mark = fileNameAsArray[1];
    String locoNumber = fileNameAsArray[2];     
    LocalDateTime endTime = LocalDateTime.parse(fileNameAsArray[3], dateFormatter);
    
    String dbUrl = "jdbc:postgresql://" + DB_URL + ":5432/" + DB_NAME;
    String query = "INSERT INTO " + DB_TABLE + "(locoid,device,endtime,filepath) VALUES (?,?,?,?)";
    
    

    try (Connection con = DriverManager.getConnection(dbUrl, DB_USER, DB_PW);
            PreparedStatement pst = con.prepareStatement(query)) {
           
           pst.setString(1, mark + "-" + locoNumber);
           pst.setString(2, device);
           pst.setObject(3, endTime);
           pst.setString(4, fileURL);
           pst.executeUpdate();

       } catch (SQLException ex) {
    	   Logger sqlLogger = Logger.getLogger(PreparedStatement.class.getName());
       }              
    return null;    
  }
}