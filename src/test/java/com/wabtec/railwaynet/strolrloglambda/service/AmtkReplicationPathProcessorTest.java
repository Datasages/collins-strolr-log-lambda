package com.wabtec.railwaynet.strolrloglambda.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

class AmtkReplicationPathProcessorTest {



    @Test
    void getReplicationPath_returnsExpectedPath() {
        LogFile lf = new LogFile(
            "AMTK",
            10,
            "CPU-3",
            LocalDateTime.of(2025, 6, 5, 4, 21, 30),
            "https://s3.amazonaws.com/test-bucket/amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/app.AMTK.10.20250605042130.log.gz"
        );

        AmtkReplicationPathProcessor processor = new AmtkReplicationPathProcessor();

        String expected = "Sorted_Logs_for_05_JUN_2025/AMTK.10.05_JUN_2025/CPU-3/";
        String actual = processor.getReplicationPath(lf);

        assertEquals(expected, actual);
    }
}
