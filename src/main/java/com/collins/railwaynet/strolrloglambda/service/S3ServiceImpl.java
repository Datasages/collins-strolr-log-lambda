package com.collins.railwaynet.strolrloglambda.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.collins.railwaynet.strolrloglambda.entity.LogFile;

@Service
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 s3Client;
    private final DateTimeFormatter logFileDateFormat = DateTimeFormatter.ofPattern("dd_MMM_yyyy");

    @Value("${data.warehouse.bucket}")
    private String dataWarehouseBucket;

    @Value("${enable.data.warehouse:false}")
    private boolean enableDataWarehouse;

    @Value("${enable.replication:false}")
    private boolean enableReplication;

    public S3ServiceImpl(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public long getFileSize(String bucket, String key) {
        return s3Client.getObjectMetadata(bucket, key).getContentLength();
    }

    @Override
    public void replicateFile(String srcBucket, String srcKey, String destBucket, LogFile logFile, String logFileName) {
        if (!enableReplication || !srcBucket.contains("mdm.amtk")) {
            return;
        }

        String formattedDate = logFileDateFormat.format(logFile.getEndTime()).toUpperCase(); // Ensure JUN
        String topFolder = "Sorted_Logs_for_" + formattedDate + "/";
        String secondFolder = logFile.getMark().toLowerCase() + "." + logFile.getLocoNumber() + "." + formattedDate + "/" + logFile.getDevice() + "/";
        String replicationKey = topFolder + secondFolder + logFileName;

        CopyObjectRequest replicationRequest = new CopyObjectRequest(srcBucket, srcKey, destBucket, replicationKey);
        s3Client.copyObject(replicationRequest);
    }

    @Override
    public void storeDataWarehouseMetadata(String bucket, LogFile logFile, String fileName, long fileSize) {
        try {
            if (!enableDataWarehouse) {
                return;
            }
            
            JSONObject metadata = new JSONObject();
            String scac = getScac(logFile.getMark());
            metadata.put("scac", scac);
            metadata.put("mark", logFile.getMark());
            metadata.put("loconumber", logFile.getLocoNumber());
            metadata.put("device", logFile.getDevice());
            metadata.put("timestamp", logFile.getEndTime().toString());
            metadata.put("filename", fileName);
            metadata.put("filesize", String.valueOf(fileSize));
            
            String jsonString = metadata.toString();
            InputStream jsonStream = new ByteArrayInputStream(jsonString.getBytes());
            ObjectMetadata dwObjectMetadata = new ObjectMetadata();
            dwObjectMetadata.setContentType("application/json");
            dwObjectMetadata.setContentLength(jsonString.length());
            
            UUID fileUUID = UUID.randomUUID();
            PutObjectRequest jsonPutObjectRequest = new PutObjectRequest(
                    dataWarehouseBucket, "locologs/" + scac + "-" + fileUUID.toString() + ".json", jsonStream, dwObjectMetadata);
            s3Client.putObject(jsonPutObjectRequest);
        } catch (JSONException ex) {
        }
    }

    private String getScac(String mark) {
        switch (mark.toUpperCase()) {
            case "VREX":
                return "vrex";
            case "NYSW":
                return "nysw";
            case "AMTK":
            case "IDTX":
            case "CDTX":
            case "RNCX":
            case "WDTX":
                return "amtk";
            default:
                return "";
        }
    }
}