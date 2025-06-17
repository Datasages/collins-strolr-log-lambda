package com.wabtec.railwaynet.strolrloglambda.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

public class AmtkReplicationPathProcessor implements ReplicationPathProcessor {

    /**
     * Generates the replication path for AMTK logs.
     * 
     * @param mark The locomotive mark.
     * @param locoId The locomotive ID.
     * @param device The device type.
     * @param date The date of the log.
     * @param month The month of the log.
     * @param year The year of the log.
     * @return The formatted replication path.
     */

    @Override
    public  String getReplicationPath(LogFile lf) {

        String mark = lf.getMark();
        int locoId = lf.getLocoNumber();
        String device = lf.getDevice();
        LocalDateTime endTime = lf.getEndTime();
        String year = String.format("%04d", endTime.getYear());
        String month = endTime.format(DateTimeFormatter.ofPattern("MMM")).toUpperCase();
        String date = String.format("%02d", endTime.getDayOfMonth());

        String dateString = String.format("%s_%s_%s", date, month, year);
        return String.format("Sorted_Logs_for_%s/%s.%s.%s/%s/", dateString, mark, locoId, dateString, device);
    }  
}
