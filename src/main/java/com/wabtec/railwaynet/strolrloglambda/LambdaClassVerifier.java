package com.wabtec.railwaynet.strolrloglambda;
@SuppressWarnings("unused")
public class LambdaClassVerifier {
    public static void main(String[] args) {
        try {
            Class.forName("com.wabtec.railwaynet.strolrloglambda.service.LogFileIndexerHandler");
            System.out.println("✅ LogFileIndexerHandler loaded successfully.");
        } catch (Throwable t) {
            System.err.println("❌ Failed to load LogFileIndexerHandler:");
            t.printStackTrace();
        }
    }
}

