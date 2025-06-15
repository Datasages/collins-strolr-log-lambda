// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.parser;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

class LogFilePathParserTest {

    private final PathParser parser = new LogFilePathParser();

    @Test
    void parse_validAIM_MDMFile_returnsPopulatedLogFile() {
        String key = "amtk-mdmlogs/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
        String bucket = "amtk-mdmlogs";

        LogFile lf = parser.parse(key, bucket);

        assertNotNull(lf);
        assertEquals("AMTK", lf.getMark());
        assertEquals(10, lf.getLocoNumber());
        assertEquals("CPU-3", lf.getDevice());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), lf.getEndTime());
        assertEquals("https://s3.amazonaws.com/amtk-mdmlogs/" + key, lf.getLogFilePath());
    }

    @Test
    void parse_validNonMdmFile_returnsPopulatedLogFile() {
        String key = "somebucket/app/VREX.63.20250605042130.log.gz";
        String bucket = "somebucket";

        LogFile lf = parser.parse(key, bucket);

        assertNotNull(lf);
        assertEquals("VREX", lf.getMark());
        assertEquals(63, lf.getLocoNumber());
        assertTrue(lf.getDevice().isEmpty());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), lf.getEndTime());
        assertEquals("https://s3.amazonaws.com/somebucket/" + key, lf.getLogFilePath());
    }

    @Test
    void parse_historyAIM_MDMFile_returnsGivenLogFile() {
        String key = "bucket/amtk.l.amtk.10:mdm/fault_history.3.20250605042130.log.gz";
        String bucket = "bucket";

        LogFile lf = parser.parse(key, bucket);

        assertNotNull(lf);
        assertEquals("AMTK", lf.getMark());
        assertEquals(10, lf.getLocoNumber());
        assertTrue(lf.getDevice().isEmpty());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), lf.getEndTime());
    }

    @Test
    void parse_invalidExtension_returnsNull() {
        String key = "bucket/path/data.txt";
        LogFile lf = parser.parse(key, "bucket");
        assertNull(lf);
    }

    @Test
    void parse_tmpPath_returnsNull() {
        String key = "bucket/tmp/my.log.gz";
        LogFile lf = parser.parse(key, "bucket");
        assertNull(lf);
    }

    @Test
    void parse_missingTimestamp_throws() {
        String key = "bucket/app.AMTK.10.log.gz";
        Exception ex = assertThrows(IllegalArgumentException.class,
                                   () -> parser.parse(key, "bucket"));
        assertTrue(ex.getMessage().contains("No valid timestamp"));
    }
    
}
