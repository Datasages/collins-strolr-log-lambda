package com.wabtec.railwaynet.strolrloglambda.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.wabtec.railwaynet.strolrloglambda.parser.LogFilePathParser;
import com.wabtec.railwaynet.strolrloglambda.repository.JdbcLogFileRepository;
import com.wabtec.railwaynet.strolrloglambda.util.S3EventTestHelper;
import com.wabtec.railwaynet.strolrloglambda.util.TestContext;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class LogFileIndexerHandlerIT {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("password");

    @SuppressWarnings("resource")
    @Container
    static final LocalStackContainer localstack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.S3)
            .withReuse(false);

    static DataSource ds;
    static S3Client s3Client;

    @BeforeAll
    @SuppressWarnings("unused")
    static void initContainers() throws Exception {
        ds = createDataSource(postgres);
        createLogTable(ds);
        s3Client = createS3Client(localstack);
    }

    @AfterEach
    @SuppressWarnings({"unused", "UseSpecificCatch"})
    void cleanUpBuckets() {
        s3Client.listBuckets().buckets().forEach(bucket -> {
            try {
                var objects = s3Client.listObjectsV2(b -> b.bucket(bucket.name()));
                for (var obj : objects.contents()) {
                    s3Client.deleteObject(o -> o.bucket(bucket.name()).key(obj.key()));
                }
                s3Client.deleteBucket(b -> b.bucket(bucket.name()));
            } catch (Exception ignored) {}
        });
    }

    private static DataSource createDataSource(PostgreSQLContainer<?> pg) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(pg.getJdbcUrl());
        dataSource.setUser(pg.getUsername());
        dataSource.setPassword(pg.getPassword());
        return dataSource;
    }

    private static void createLogTable(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("""
                CREATE TABLE logfileindex (
                  id SERIAL PRIMARY KEY,
                  mark VARCHAR(255),
                  locoNumber INT,
                  device VARCHAR(255),
                  endTime TIMESTAMP,
                  logFilePath TEXT
                )
            """);
        }
    }

    private static S3Client createS3Client(LocalStackContainer ls) {
        return S3Client.builder()
            .endpointOverride(ls.getEndpointOverride(LocalStackContainer.Service.S3))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ls.getAccessKey(), ls.getSecretKey())))
            .region(Region.of(ls.getRegion()))
            .build();
    }

    @Test
    void testFullIntegrationFlow() throws Exception {
        // 👇 Match the file key in your test JSON file
        String key = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";
      

        s3Client.createBucket(b -> b.bucket("test-bucket"));
        s3Client.createBucket(b -> b.bucket("dest-bucket"));
        s3Client.putObject(b -> b.bucket("test-bucket").key(key),
                   RequestBody.fromString("dummy-content"));

        var handler = new LogFileIndexerHandler(
            new LogFilePathParser(),
            new JdbcLogFileRepository(ds),
            new S3ServiceImpl(s3Client),
            true,
            "dest-bucket"
        );

        String json = Files.readString(Path.of("src/test/resources/s3-event.json"));
        S3Event event = S3EventTestHelper.loadEvent(json);
        String result = handler.handleRequest(event, new NoOpContext());

        assertEquals("Success", result);

        // ✅ Database assertions
        try (Connection c = ds.getConnection();
             ResultSet rs = c.createStatement()
                .executeQuery("SELECT mark, locoNumber, device, endTime FROM logfileindex")) {
            assertTrue(rs.next());
            assertEquals("AMTK", rs.getString("mark"));
            assertEquals(10, rs.getInt("locoNumber"));
            assertEquals("CPU-3", rs.getString("device"));
            assertEquals(LocalDateTime.of(2025, 6, 5, 4, 21, 30),
                         rs.getObject("endTime", LocalDateTime.class));
            assertFalse(rs.next(), "Expected only one row in the table");
        }

        // ✅ S3 replication check
        var resp = s3Client.listObjectsV2(b -> b.bucket("dest-bucket"));
        assertFalse(resp.contents().isEmpty());
        assertEquals("app.AMTK.10.20250605042130.log.gz", resp.contents().get(0).key());
    }

    @Test
    @SuppressWarnings("UseSpecificCatch")
    void testIntegration_noReplication() throws Exception {
        try {
            s3Client.deleteBucket(b -> b.bucket("dest-bucket"));
        } catch (Exception e) {
            // Ignore if not found
        }
        // 👇 Match the file key in your test JSON file
        // This key should match the one in your S3 event JSON
       String key = "amtk.l.amtk.10:mdm/2025/JUN/05/01:24-CPU-3/disk/var/log/app.AMTK.10.20250605042130.log.gz";


        s3Client.createBucket(b -> b.bucket("test-bucket"));
        s3Client.putObject(b -> b.bucket("test-bucket").key(key), RequestBody.fromString("dummy-content"));

        var handler = new LogFileIndexerHandler(
            new LogFilePathParser(),
            new JdbcLogFileRepository(ds),
            new S3ServiceImpl(s3Client),
            false,  // replication disabled
            null    // no dest bucket needed
        );

        S3Event event = EventLoader.loadS3Event("/s3-event.json");
        String result = handler.handleRequest(event, stubContext());

        assertEquals("Success", result);

        // ✅ Check DB for saved log file metadata
        try (Connection c = ds.getConnection();
            ResultSet rs = c.createStatement()
                .executeQuery("SELECT mark,locoNumber,device,endTime FROM logfileindex")) {
            assertTrue(rs.next());
            assertEquals("AMTK", rs.getString("mark"));
            assertEquals(10, rs.getInt("locoNumber"));
            assertEquals("CPU-3", rs.getString("device"));
            assertEquals(LocalDateTime.of(2025,6,5,4,21,30),
                        rs.getObject("endTime", LocalDateTime.class));
        }

        // ✅ Ensure no file was replicated to another bucket
        var resp = s3Client.listObjectsV2(b -> b.bucket("test-bucket"));
        assertFalse(resp.contents().isEmpty());
        // No "dest-bucket" was created
        var allBuckets = s3Client.listBuckets().buckets();
        boolean destExists = allBuckets.stream()
                                    .anyMatch(b -> b.name().equals("dest-bucket"));
        assertFalse(destExists, "dest-bucket should not be created");
    }

   @Test
    @SuppressWarnings("UseSpecificCatch")
void testIntegration_unparsableKey_skipped() throws Exception {
    try {
        s3Client.deleteBucket(b -> b.bucket("dest-bucket"));
    } catch (Exception e) {
        // Ignore if not found
    }
    String key = "file.log.gz"; // clearly invalid
    s3Client.createBucket(b -> b.bucket("test-bucket"));
    s3Client.putObject(b -> b.bucket("test-bucket").key(key),
                       RequestBody.fromString("dummy-content"));

    var handler = new LogFileIndexerHandler(
        new LogFilePathParser(),
        new JdbcLogFileRepository(ds),
        new S3ServiceImpl(s3Client),
        true,
        "dest-bucket"
    );

    S3Event event = S3EventTestHelper.fromKey("test-bucket", key);
    String result = handler.handleRequest(event, new TestContext());

    assertEquals("Skipped", result);
}




    // Simple no-op context to avoid NPEs
    static class NoOpContext implements Context {
        @Override public String getAwsRequestId() { return "req"; }
        @Override public String getLogGroupName() { return "log-group"; }
        @Override public String getLogStreamName() { return "log-stream"; }
        @Override public String getFunctionName() { return "fn"; }
        @Override public String getFunctionVersion() { return "v"; }
        @Override public String getInvokedFunctionArn() { return "arn"; }
        @Override public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        @Override public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        @Override public int getRemainingTimeInMillis() { return 300000; }
        @Override public int getMemoryLimitInMB() { return 512; }
        @Override
        public com.amazonaws.services.lambda.runtime.LambdaLogger getLogger() {
            return new com.amazonaws.services.lambda.runtime.LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println("[LOG] " + message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println("[LOG] " + new String(message));
                }
            };
        }
    }

    private static Context stubContext() {
    return new Context() {
        @Override public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println("[LambdaLog] " + message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println("[LambdaLog] " + new String(message));
                }
            };
        }
        @Override public String getAwsRequestId() { return "test-request"; }
        @Override public String getLogGroupName() { return "test-log-group"; }
        @Override public String getLogStreamName() { return "test-log-stream"; }
        @Override public String getFunctionName() { return "test-function"; }
        @Override public String getFunctionVersion() { return "1.0"; }
        @Override public String getInvokedFunctionArn() { return "arn:aws:lambda:test"; }
        @Override public CognitoIdentity getIdentity() { return null; }
        @Override public ClientContext getClientContext() { return null; }
        @Override public int getRemainingTimeInMillis() { return 300000; }
        @Override public int getMemoryLimitInMB() { return 512; }
    };
}
}
