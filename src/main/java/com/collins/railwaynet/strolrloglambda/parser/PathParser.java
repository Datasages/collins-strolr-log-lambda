package com.collins.railwaynet.strolrloglambda.parser;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;

public interface PathParser {
    LogFile parse(String s3Key, String bucket);
}
