// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
// Repository Interface
package com.wabtec.railwaynet.strolrloglambda.repository;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

public interface LogFileRepository {
    void save(LogFile logFile);
}
