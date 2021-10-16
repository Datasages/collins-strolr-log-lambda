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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// AIM Log Path
// /amtk-mdmlogs/amtk.l.amtk.53:mdm/2021/OCT/11/04:57-CPU-1/disk/var/log/app.AMTK.53.20211011081112.log.gz
// /amtk-mdmlogs/amtk.l.amtk.53:mdm/2021/OCT/11/04:57-CPU-1/disk/var/log/fault_history.3.20210925224041.log.gz
// /amtk-mdmlogs/amtk.l.amtk.53:mdm/2021/OCT/11/04:57-CPU-3/disk/var/log/aas.LOCO.XXXX.20210924033416.log.gz

// CAS Log Path
// /amtk-mdmlogs/vrex/VREX63/CPU-1/disk/var/log/app.VREX.63.20210919182601.log.gz
// /amtk-mdmlogs/vrex/VREX63/CPU-2/disk/var/log/fault_history.2.20211013103455.log.gz


public class LogFileIndexerService implements RequestHandler< S3Event, String> {
	private LambdaLogger logger;
	private static String PATH_DELIMITER = "/";
    private static String FILE_DELIMITER = "\\.";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    private static final String SQL_STATEMENT = "INSERT INTO logfileindex" +
            "  (mark, locoNumber, device, endTime, logFilePath) VALUES " +
            " (?, ?, ?, ?, ?);";
      
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
    String logFileName = fullPathAsArray[pathDepth-1];
    Pattern filePattern = Pattern.compile(FILE_DELIMITER);
    String[] fileNameAsArray = filePattern.split(logFileName);
        
    String mark = "";
    int locoNumber = 0;
    String device = "";   
    LocalDateTime endTime = null;
    String fileURL = "https://" + srcBucket + "s3.amazonaws.com" +"/" + srcKey; 
    
    //find Device
    if (srcKey.contains("CPU-1")) {
    	device = "CPU-1";
    }
    if (srcKey.contains("CPU-2")) {
    	device = "CPU-2";
    }
    if (srcKey.contains("CPU-3")) {
    	device = "CPU-3";
    }
    if (srcKey.contains("CDU-1")) {
    	device = "CDU-1";
    }
       
 // Determine if AIM MDM or CAS MDM
	//AIM MDM
    if (srcKey.contains("mdm")) {
    	
    	// fault history doesn't have mark and locoID
		if (
				logFileName.contains("fault_history") ||
				logFileName.contains("event_history") ||
				logFileName.contains("failure_history") 
				) {
			logger.log("History file: " + logFileName);
			for(String folder: fullPathAsArray) {
    			if (folder.contains("mdm")) {
    				logger.log("loco name folder: " + folder);
    				String[] locoIdArray = folder.split("\\.");
    				mark = locoIdArray[2];
    				logger.log("AIM Mark " + mark);
    				locoNumber = Integer.parseInt(locoIdArray[3].substring(0, locoIdArray[3].length() -4));
    				logger.log("AIM Loco Number: " + locoNumber );
    			}
    		}			
	        endTime = LocalDateTime.parse(fileNameAsArray[2], dateFormatter);
		}
		else if (
				logFileName.contains("depart_test") 
				) {
			logger.log("Departure Test file: " + logFileName);
			for(String folder: fullPathAsArray) {
    			if (folder.contains("mdm")) {
    				logger.log("loco name folder: " + folder);
    				String[] locoIdArray = folder.split("\\.");
    				mark = locoIdArray[2];
    				logger.log("AIM Mark " + mark);
    				locoNumber = Integer.parseInt(locoIdArray[3].substring(0, locoIdArray[3].length() -4));
    				logger.log("AIM Loco Number: " + locoNumber );
    			}
    		}			
	        endTime = LocalDateTime.parse(fileNameAsArray[1], dateFormatter);
		}
		else if (logFileName.contains("LOCO")) {
			logger.log("AAS LOCO file: " + logFileName);
			for(String folder: fullPathAsArray) {
    			if (folder.contains("mdm")) {
    				logger.log("loco name folder: " + folder);
    				String[] locoIdArray = folder.split("\\.");
    				mark = locoIdArray[2];
    				logger.log("AIM Mark " + mark);
    				locoNumber = Integer.parseInt(locoIdArray[3].substring(0, locoIdArray[3].length() -4));
    				logger.log("AIM Loco Number: " + locoNumber );
    			}
    		}			
	        endTime = LocalDateTime.parse(fileNameAsArray[3], dateFormatter);
		}
		else {
	    	mark = fileNameAsArray[1];
	    	locoNumber = Integer.parseInt(fileNameAsArray[2]);
	        endTime = LocalDateTime.parse(fileNameAsArray[3], dateFormatter);
		}
	
	}
	else {
		//CAS MDM
		
    	// fault history doesn't have mark and locoID
		if (
				logFileName.contains("fault_history") ||
				logFileName.contains("event_history") ||
				logFileName.contains("failure_history") 
				) {
    		int depthCounter = 0;
			String markLocoID = "";
    		for(String folder: fullPathAsArray) {
    			if (folder.contains("CPU-")) {
    				markLocoID = fullPathAsArray[depthCounter -1];
    				logger.log("CAS markLocoID " + markLocoID);
    				break;
    			}
    			depthCounter += 1;
    		}
    		String[] markLocoIDArray = markLocoID.split("(?<=\\D)(?=\\d)");
    		mark = markLocoIDArray[0];
    		logger.log("CAS Mark: " + mark);
    		locoNumber = Integer.parseInt(markLocoIDArray[1]);
    		logger.log("CAS Loco Number: " + locoNumber);
	        endTime = LocalDateTime.parse(fileNameAsArray[2], dateFormatter);
		}
		else if (
				logFileName.contains("depart_test") 
				) {
    		int depthCounter = 0;
			String markLocoID = "";
    		for(String folder: fullPathAsArray) {
    			if (folder.contains("CPU-")) {
    				markLocoID = fullPathAsArray[depthCounter -1];
    				logger.log("CAS markLocoID " + markLocoID);
    				break;
    			}
    			depthCounter += 1;
    		}
    		String[] markLocoIDArray = markLocoID.split("(?<=\\D)(?=\\d)");
    		mark = markLocoIDArray[0];
    		logger.log("CAS Mark: " + mark);
    		locoNumber = Integer.parseInt(markLocoIDArray[1]);
    		logger.log("CAS Loco Number: " + locoNumber);
	        endTime = LocalDateTime.parse(fileNameAsArray[1], dateFormatter);
		}
		else if (logFileName.contains("LOCO")) {
    		int depthCounter = 0;
			String markLocoID = "";
    		for(String folder: fullPathAsArray) {
    			if (folder.contains("CPU-")) {
    				markLocoID = fullPathAsArray[depthCounter -1];
    				logger.log("CAS markLocoID " + markLocoID);
    				break;
    			}
    			depthCounter += 1;
    		}
    		String[] markLocoIDArray = markLocoID.split("(?<=\\D)(?=\\d)");
    		mark = markLocoIDArray[0];
    		logger.log("CAS Mark: " + mark);
    		locoNumber = Integer.parseInt(markLocoIDArray[1]);
    		logger.log("CAS Loco Number: " + locoNumber);
	        endTime = LocalDateTime.parse(fileNameAsArray[2], dateFormatter);
		}
		else {
	    	mark = fileNameAsArray[1];
	    	locoNumber = Integer.parseInt(fileNameAsArray[2]);
	        endTime = LocalDateTime.parse(fileNameAsArray[3], dateFormatter);
		}
	}        
 
    LogFile logfile = new LogFile(mark, locoNumber, device, endTime, fileURL);    
    logger.log("Log file: " + logfile.toString());
    
    try (Connection connection = DriverManager.getConnection(
    		"jdbc:postgresql://m-ris-postgres-db.cjtxx3u946t0.us-east-1.rds.amazonaws.com:5432/strolr-logfiledb", 
    		"strolr_logfiledb_writer", 
    		"ygZ7XuENjsBHwVyy"
    	); 
    	PreparedStatement preparedStatement = connection.prepareStatement(SQL_STATEMENT)) {
        preparedStatement.setString(1, logfile.getMark());
        preparedStatement.setInt(2, logfile.getLocoNumber());
        preparedStatement.setString(3, logfile.getDevice());
        preparedStatement.setObject(4, logfile.getEndTime());
        preparedStatement.setString(5, logfile.getLogFilePath());

        System.out.println(preparedStatement);
        // Step 3: Execute the query or update query
        preparedStatement.executeUpdate();
  } catch (SQLException e) {
	  logger.log("SQL Exception");
	  printSQLException(e);
  }
    
//    Transaction transaction = null;
//    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//        transaction = session.beginTransaction();
//        session.save(logfile);
//        transaction.commit();
//        session.close();
//    } catch (Exception e) {
//        if (transaction != null) {
//            transaction.rollback();
//        }
//        e.printStackTrace();
//    }                 
    return null;    
  }
  
  public static void printSQLException(SQLException ex) {
	  
      for (Throwable e: ex) {
          if (e instanceof SQLException) {
              e.printStackTrace(System.err);
              System.err.println("SQLState: " + ((SQLException) e).getSQLState());
              System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
              System.err.println("Message: " + e.getMessage());
              Throwable t = ex.getCause();
              while (t != null) {
                  System.out.println("Cause: " + t);
                  t = t.getCause();
              }
          }
      }
  }
}