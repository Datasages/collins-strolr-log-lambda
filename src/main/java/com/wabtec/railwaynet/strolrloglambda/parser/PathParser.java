package com.wabtec.railwaynet.strolrloglambda.parser;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;

public interface PathParser {
    LogFile parse(String s3Key, String bucket);
}
