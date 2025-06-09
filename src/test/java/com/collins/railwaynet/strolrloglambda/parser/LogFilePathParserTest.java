package com.collins.railwaynet.strolrloglambda.parser;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;

class LogFilePathParserTest {

    private final PathParser pathParser = new LogFilePathParser();

    @Test
    void testParse_AIM_MDM_ValidKey() {
        String s3Key = "amtk-mdmlogs/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
        String bucket = "amtk-mdmlogs";

        LogFile logFile = pathParser.parse(s3Key, bucket);

        assertNotNull(logFile);
        assertEquals("AMTK", logFile.getMark()); // Updated to expect uppercase AMTK
        assertEquals(10, logFile.getLocoNumber());
        assertEquals("CPU-3", logFile.getDevice());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), logFile.getEndTime());
        assertEquals("https://s3.amazonaws.com/amtk-mdmlogs/" + s3Key, logFile.getLogFilePath());
    }

    @Test
    void testParse_CAS_MDM_ValidKey() {
        String s3Key = "amtk-mdmlogs/vrex/VREX63/CPU-2/disk/var/log/app.VREX.63.20250605042130.log.gz";
        String bucket = "amtk-mdmlogs";

        LogFile logFile = pathParser.parse(s3Key, bucket);

        assertNotNull(logFile);
        assertEquals("VREX", logFile.getMark());
        assertEquals(63, logFile.getLocoNumber());
        assertEquals("CPU-2", logFile.getDevice());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), logFile.getEndTime());
    }

    
    @Test
    void testParse_HistoryFile() {
        String s3Key = "amtk-mdmlogs/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/fault_history.3.20250605042130.log.gz";
        String bucket = "amtk-mdmlogs";

        LogFile logFile = pathParser.parse(s3Key, bucket);

        assertNotNull(logFile);
        assertEquals("AMTK", logFile.getMark()); // Updated to expect uppercase AMTK
        assertEquals(10, logFile.getLocoNumber());
        assertEquals("CPU-3", logFile.getDevice());
        assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30), logFile.getEndTime());
    }

    @Test
    void testParse_SkipInvalidKey() {
        String s3Key = "amtk-mdmlogs/tmp/invalid.txt";
        String bucket = "amtk-mdmlogs";

        LogFile logFile = pathParser.parse(s3Key, bucket);

        assertNull(logFile);
    }
}