// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.parser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

public class LogFilePathParser implements PathParser {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern DESIRED_FILES = Pattern.compile(".*log\\.gz$");
    private static final Pattern MDM_TMP_FILES = Pattern.compile(".*/tmp.*");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{14}");
    private static final Pattern AIM_MDM_FOLDER_PATTERN =
        Pattern.compile("amtk\\.l\\.(\\w+)\\.(\\d+):mdm", Pattern.CASE_INSENSITIVE);

    @Override
    public LogFile parse(String s3Key, String bucket) {
        String decoded = URLDecoder.decode(s3Key, StandardCharsets.UTF_8);
        if (!DESIRED_FILES.matcher(decoded).matches() || MDM_TMP_FILES.matcher(decoded).matches()) {
            return null;
        }

        String[] parts = decoded.split("/");
        String filename = parts[parts.length - 1];
        String[] fileSegments = filename.split("\\.");

        String device = extractDevice(decoded);
        String fileUrl = "https://s3.amazonaws.com/" + bucket + "/" + s3Key;

        boolean isMdm = decoded.contains("mdm");
        boolean isHistory = filename.startsWith("app.") ||
        					filename.startsWith("chr.") ||
        					filename.startsWith("tdat.") ||
                            filename.contains("fault_history") ||
                            filename.contains("event_history") ||
                            filename.contains("failure_history") ||
                            filename.contains("depart_test") ||
                            filename.contains("LOCO");

        String mark;
        int locoNumber;
        LocalDateTime endTime;

        if (isMdm) {
            if (isHistory) {
                String[] ml = findMdmFolder(parts);
                mark = ml[0];
                locoNumber = Integer.parseInt(ml[1]);
                endTime = extractTimestamp(filename);
            } else {
                try {
                    mark = fileSegments[0];
                    locoNumber = Integer.parseInt(fileSegments[1]);
                    endTime = LocalDateTime.parse(fileSegments[2], DATE_FORMATTER);
                } catch (NumberFormatException | DateTimeParseException e) {
                    throw new DateTimeParseException("No valid timestamp or locoNumber in filename: " + filename,
                                                     filename, 0, e);
                }
            }
        } else {
            if (isHistory) {
                endTime = extractTimestamp(filename);
                // No CAS support, fallback or throw
                mark = fileSegments.length > 1 ? fileSegments[1] : "";
                locoNumber = fileSegments.length > 2 ? Integer.parseInt(fileSegments[2]) : 0;
            } else {
                try {
                    mark = fileSegments[0];
                    locoNumber = Integer.parseInt(fileSegments[1]);
                    endTime = LocalDateTime.parse(fileSegments[2], DATE_FORMATTER);
                } catch (NumberFormatException | DateTimeParseException e) {
                    throw new DateTimeParseException("No valid timestamp or locoNumber in filename: " + filename,
                                                     filename, 0, e);
                }
            }
        }

        if (endTime == null) {
            throw new IllegalArgumentException("No valid timestamp in " + filename);
        }

        return new LogFile(mark, locoNumber, device, endTime, fileUrl);
    }

    private String extractDevice(String decoded) {
        if (decoded.contains("CPU-1")) return "CPU-1";
        if (decoded.contains("CPU-2")) return "CPU-2";
        if (decoded.contains("CPU-3")) return "CPU-3";
        if (decoded.contains("CDU-1")) return "CDU-1";
        return "";
    }

    private String[] findMdmFolder(String[] parts) {
        for (String p : parts) {
            Matcher m = AIM_MDM_FOLDER_PATTERN.matcher(p);
            if (m.matches()) {
                return new String[]{m.group(1).toUpperCase(), m.group(2)};
            }
        }
        return new String[]{"", "0"};
    }

    private LocalDateTime extractTimestamp(String text) {
        Matcher m = TIMESTAMP_PATTERN.matcher(text);
        if (m.find()) {
            return LocalDateTime.parse(m.group(), DATE_FORMATTER);
        }
        return null;
    }
}
