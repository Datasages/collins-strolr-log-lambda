// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Thread-safe, TTL-bounded in-memory cache over AWS Secrets Manager (SDK v2),
 * implementing {@link SecretResolver}.
 *
 * <p>The {@link SecretsManagerClient} is injectable via constructor so the resolve path
 * can be unit-tested with a stub. The no-arg constructor lazily creates a real client on
 * first use, so AWS region/credential resolution doesn't run at construction (keeping the
 * no-arg path cheap and safe in a test JVM with no AWS region). One instance is created at
 * Lambda cold-start and reused across warm invocations, so the instance cache persists for
 * the container's lifetime — same caching behavior as before, without static state.
 */
public class SecretManagerCache implements SecretResolver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SecretManagerCache.class);
    private static final Duration TTL = Duration.ofMinutes(60);

    private final Map<String, CachedSecret> cache = new ConcurrentHashMap<>();
    private SecretsManagerClient client;

    /** Production: the real client is created lazily on first {@link #resolve}. */
    public SecretManagerCache() {
        this.client = null;
    }

    /** Inject a client (tests, or alternate wiring). */
    public SecretManagerCache(SecretsManagerClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    private synchronized SecretsManagerClient client() {
        if (client == null) {
            client = SecretsManagerClient.create();
        }
        return client;
    }

    @Override
    public String resolve(String secretId) {
        CachedSecret cs = cache.computeIfAbsent(secretId, id -> new CachedSecret());
        synchronized (cs) {
            if (cs.isExpired()) {
                try {
                    GetSecretValueResponse resp = client().getSecretValue(
                        GetSecretValueRequest.builder().secretId(secretId).build()
                    );
                    cs.update(resp.secretString());
                    LOGGER.debug("Retrieved secret '{}' from Secrets Manager", secretId);
                } catch (AwsServiceException | SdkClientException e) {
                    // SECURITY: fail closed. A secret that cannot be retrieved is a fatal
                    // error — never fall back to a default/hardcoded credential.
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
