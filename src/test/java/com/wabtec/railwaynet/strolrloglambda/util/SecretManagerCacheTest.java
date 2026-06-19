// Copyright Wabtec Inc. 2025. All rights reserved
package com.wabtec.railwaynet.strolrloglambda.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Regression tests for {@link SecretManagerCache}.
 *
 * The load-bearing case is {@code getSecret_failsClosed_whenUnreachable}: it locks in the
 * security fix that replaced a hardcoded fail-open "default-db-password" fallback with a
 * fail-closed throw. If a future refactor reintroduces any fallback, this test fails.
 */
class SecretManagerCacheTest {

    @BeforeEach
    @AfterEach
    void coldCache() {
        SecretManagerCache.resetForTesting();
    }

    @Test
    void getSecret_returnsValue_whenClientSucceeds() {
        SecretsManagerClient stub = mock(SecretsManagerClient.class);
        when(stub.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString("resolved-secret-value").build());
        SecretManagerCache.setClientForTesting(stub);

        assertEquals("resolved-secret-value", SecretManagerCache.getSecret("some-secret-name"));
    }

    @Test
    void getSecret_failsClosed_whenUnreachable() {
        SecretsManagerClient stub = mock(SecretsManagerClient.class);
        when(stub.getSecretValue(any(GetSecretValueRequest.class)))
            .thenThrow(SdkClientException.create("simulated Secrets Manager outage"));
        SecretManagerCache.setClientForTesting(stub);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> SecretManagerCache.getSecret("db-password-secret"));

        // Fail closed: surfaces the secret *name*, never a fallback credential value.
        assertTrue(ex.getMessage().contains("db-password-secret"),
            "exception should name the secret that could not be retrieved");
        assertFalse(ex.getMessage().contains("default-db-password"),
            "fail-open fallback credential must never reappear");
    }
}
