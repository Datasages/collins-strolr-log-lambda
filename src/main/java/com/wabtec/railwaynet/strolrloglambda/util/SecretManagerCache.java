package com.wabtec.railwaynet.strolrloglambda.util;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple thread-safe in-memory cache for Secrets Manager (pure SDK v2).
 */
public class SecretManagerCache {
    private static final Duration TTL = Duration.ofMinutes(60);
    private static final SecretsManagerClient CLIENT = SecretsManagerClient.create();
    private static final Map<String, CachedSecret> CACHE = new ConcurrentHashMap<>();

    public static String getSecret(String secretId) {
        CachedSecret cs = CACHE.computeIfAbsent(secretId, id -> new CachedSecret());
        synchronized (cs) {
            if (cs.isExpired()) {
                GetSecretValueResponse resp = CLIENT.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretId).build()
                );
                cs.update(resp.secretString());
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
