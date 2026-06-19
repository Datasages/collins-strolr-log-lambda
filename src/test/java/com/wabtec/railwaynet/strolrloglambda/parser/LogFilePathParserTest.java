// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

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

    @Test
    void parse_nonMdm_extractsDeviceCpu2() {
        String key = "bucket/CPU-2/MK.5.20240101010101.log.gz";

        LogFile lf = parser.parse(key, "bucket");

        assertNotNull(lf);
        assertEquals("MK", lf.getMark());
        assertEquals(5, lf.getLocoNumber());
        assertEquals("CPU-2", lf.getDevice());
        assertEquals(LocalDateTime.of(2024, 1, 1, 1, 1, 1), lf.getEndTime());
    }

    @Test
    void parse_nonMdm_extractsDeviceCdu1() {
        String key = "bucket/CDU-1/MK.7.20240202020202.log.gz";

        LogFile lf = parser.parse(key, "bucket");

        assertNotNull(lf);
        assertEquals("CDU-1", lf.getDevice());
        assertEquals(7, lf.getLocoNumber());
    }

    @Test
    void parse_nonMdmHistory_takesMarkAndLocoFromFilenameSegments() {
        // !isMdm && isHistory branch: mark = segment[1], loco = segment[2], timestamp scanned.
        String key = "bucket/logs/event_history.NJTR.55.20230303030303.log.gz";

        LogFile lf = parser.parse(key, "bucket");

        assertNotNull(lf);
        assertEquals("NJTR", lf.getMark());
        assertEquals(55, lf.getLocoNumber());
        assertEquals(LocalDateTime.of(2023, 3, 3, 3, 3, 3), lf.getEndTime());
        assertTrue(lf.getDevice().isEmpty());
    }

    @Test
    void parse_mdmHistory_noMatchingFolder_fallsBackToEmptyMarkZeroLoco() {
        // isMdm && isHistory, but no folder matches the amtk.l.<mark>.<loco>:mdm pattern.
        String key = "bucket/somemdmfolder/LOCO.20250505050505.log.gz";

        LogFile lf = parser.parse(key, "bucket");

        assertNotNull(lf);
        assertTrue(lf.getMark().isEmpty());
        assertEquals(0, lf.getLocoNumber());
        assertEquals(LocalDateTime.of(2025, 5, 5, 5, 5, 5), lf.getEndTime());
    }

    @Test
    void parse_mdmNonHistory_nonNumericLoco_throws() {
        // isMdm && !isHistory: a non-numeric loco segment is wrapped as DateTimeParseException.
        String key = "bucket/xmdmx/MK.NOTNUM.20250101000000.log.gz";

        assertThrows(DateTimeParseException.class, () -> parser.parse(key, "bucket"));
    }
}
