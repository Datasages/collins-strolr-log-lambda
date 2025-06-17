package com.wabtec.railwaynet.strolrloglambda;

import org.slf4j.LoggerFactory;

public class LambdaClassVerifier {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LambdaClassVerifier.class);
    public static void main(String[] args) {
        try {
            Class.forName("com.wabtec.railwaynet.strolrloglambda.service.LogFileIndexerHandler");
            System.out.println("✅ LogFileIndexerHandler loaded successfully.");
        } catch (ClassNotFoundException t) {
            System.err.println("❌ Failed to load LogFileIndexerHandler:");
        }
                LOGGER.debug("This is a DEBUG log");
        LOGGER.info("This is an INFO log");
        LOGGER.warn("This is a WARN log");
        System.out.println("System.out still works");
        System.out.println("Log4j2 config: " + System.getProperty("log4j.configurationFile"));
        
    }
}
