// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Simple thread-safe in-memory cache for Secrets Manager (pure SDK v2).
 */
public class SecretManagerCache {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SecretManagerCache.class);
    private static final Duration TTL = Duration.ofMinutes(60);
    private static final Map<String, CachedSecret> CACHE = new ConcurrentHashMap<>();

    // Lazily constructed so the AWS region/credential resolution in
    // SecretsManagerClient.create() doesn't run at class-load (which would fail in
    // a test JVM with no AWS region). Package-private + injectable as a test seam —
    // see setClientForTesting / resetForTesting. Not part of the public API.
    private static SecretsManagerClient client;

    private static synchronized SecretsManagerClient client() {
        if (client == null) {
            client = SecretsManagerClient.create();
        }
        return client;
    }

    /** Test seam: inject a stub SecretsManagerClient. Package-private by design. */
    static synchronized void setClientForTesting(SecretsManagerClient testClient) {
        client = testClient;
    }

    /** Test seam: drop all cached secrets so each test starts from a cold cache. */
    static void resetForTesting() {
        CACHE.clear();
    }

    public static String getSecret(String secretId) {
        CachedSecret cs = CACHE.computeIfAbsent(secretId, id -> new CachedSecret());
        synchronized (cs) {
            if (cs.isExpired()) {
                try {
                    GetSecretValueResponse resp = client().getSecretValue(
                        GetSecretValueRequest.builder().secretId(secretId).build()
                    );
                    cs.update(resp.secretString());
                    LOGGER.debug("Retrieved secret '{}' from Secrets Manager", secretId);
                } catch (AwsServiceException | SdkClientException e) {
                    // SECURITY: fail closed. The previous implementation fell back to a
                    // hardcoded "default-db-password" when Secrets Manager was unreachable,
                    // which (a) shipped a known credential inside the production fat JAR and
                    // (b) masked outages by attempting DB connections with a bogus password.
                    // A secret that cannot be retrieved is a fatal error — surface it.
                    LOGGER.error("Failed to retrieve secret '{}' from Secrets Manager", secretId, e);
                    throw new IllegalStateException("Could not retrieve secret: " + secretId, e);
                }
            }
            return cs.value;
        }
    }



    private static class CachedSecret {
        volatile String value = null;
        volatile Instant expiresAt = Instant.EPOCH;

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        void update(String newValue) {
            value = newValue;
            expiresAt = Instant.now().plus(TTL);
        }
    }
}
