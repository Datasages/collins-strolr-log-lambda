package com.collins.railwaynet.strolrloglambda.parser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;

@Component
public class LogFilePathParser implements PathParser {

    private static final String PATH_DELIMITER = "/";
    private static final String FILE_DELIMITER = "\\.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern DESIRED_FILES = Pattern.compile(".*log\\.gz");
    private static final Pattern MDM_TMP_FILES = Pattern.compile(".*/tmp.*");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{14}");
    private static final Pattern AIM_MDM_FOLDER_PATTERN = Pattern.compile("amtk\\.l\\.(\\w+)\\.(\\d+):mdm", Pattern.CASE_INSENSITIVE);

    @Override
    public LogFile parse(String s3Key, String bucket) {
        String decodedKey = URLDecoder.decode(s3Key, StandardCharsets.UTF_8);

        // Skip undesired files
        if (!DESIRED_FILES.matcher(decodedKey).matches() || MDM_TMP_FILES.matcher(decodedKey).matches()) {
            return null;
        }

        String[] pathParts = decodedKey.split(PATH_DELIMITER);
        String logFileName = pathParts[pathParts.length - 1];
        String[] fileParts = logFileName.split(FILE_DELIMITER);

        String mark = "";
        int locoNumber = 0;
        String device = extractDevice(decodedKey);
        LocalDateTime endTime = null;
        String fileUrl = "https://s3.amazonaws.com/" + bucket + "/" + s3Key;

        if (decodedKey.contains("mdm")) {
            // AIM MDM
            if (isHistoryFile(logFileName) || logFileName.contains("depart_test") || logFileName.contains("LOCO")) {
                for (String folder : pathParts) {
                    Matcher matcher = AIM_MDM_FOLDER_PATTERN.matcher(folder);
                    if (matcher.matches()) {
                        mark = matcher.group(1); // Preserve case, e.g., AMTK
                        locoNumber = Integer.parseInt(matcher.group(2)); // e.g., 10
                        break;
                    }
                }
                endTime = parseEndTime(fileParts, logFileName);
            } else {
                mark = fileParts[1]; // Preserve case, e.g., AMTK
                locoNumber = Integer.parseInt(fileParts[2]);
                endTime = LocalDateTime.parse(fileParts[3], DATE_FORMATTER);
            }
        } else {
            // CAS MDM
            if (isHistoryFile(logFileName) || logFileName.contains("depart_test") || logFileName.contains("LOCO")) {
                String markLocoId = extractMarkLocoId(pathParts);
                String[] markLocoArray = markLocoId.split("(?<=\\D)(?=\\d)");
                mark = markLocoArray[0];
                locoNumber = Integer.parseInt(markLocoArray[1]);
                endTime = parseEndTime(fileParts, logFileName);
            } else {
                mark = fileParts[1];
                locoNumber = Integer.parseInt(fileParts[2]);
                endTime = LocalDateTime.parse(fileParts[3], DATE_FORMATTER);
            }
        }

        if (endTime == null) {
            throw new IllegalArgumentException("Failed to parse endTime from S3 key: " + s3Key);
        }

        return new LogFile(mark, locoNumber, device, endTime, fileUrl);
    }

    private String extractDevice(String key) {
        if (key.contains("CPU-1")) return "CPU-1";
        if (key.contains("CPU-2")) return "CPU-2";
        if (key.contains("CPU-3")) return "CPU-3";
        if (key.contains("CDU-1")) return "CDU-1";
        return "";
    }

    private boolean isHistoryFile(String fileName) {
        return fileName.contains("fault_history") ||
               fileName.contains("event_history") ||
               fileName.contains("failure_history");
    }

    private String extractMarkLocoId(String[] pathParts) {
        for (int i = 0; i < pathParts.length; i++) {
            if (pathParts[i].contains("CPU-")) {
                return pathParts[i - 1];
            }
        }
        return "";
    }

    private LocalDateTime parseEndTime(String[] fileParts, String fileName) {
        if (fileName.contains("depart_test")) {
            return LocalDateTime.parse(fileParts[1], DATE_FORMATTER);
        } else if (fileName.contains("LOCO")) {
            return LocalDateTime.parse(fileParts[2], DATE_FORMATTER);
        } else if (isHistoryFile(fileName)) {
            Matcher matcher = TIMESTAMP_PATTERN.matcher(fileName);
            if (matcher.find()) {
                return LocalDateTime.parse(matcher.group(), DATE_FORMATTER);
            }
            throw new IllegalArgumentException("No valid timestamp found in history file: " + fileName);
        } else {
            for (String part : fileParts) {
                if (TIMESTAMP_PATTERN.matcher(part).matches()) {
                    return LocalDateTime.parse(part, DATE_FORMATTER);
                }
            }
            throw new IllegalArgumentException("No valid timestamp found in file: " + fileName);
        }
    }
}