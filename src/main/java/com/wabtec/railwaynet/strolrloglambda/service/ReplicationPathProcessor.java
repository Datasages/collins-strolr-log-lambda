// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.service;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

public interface ReplicationPathProcessor {
    String getReplicationPath(LogFile lf);
}
