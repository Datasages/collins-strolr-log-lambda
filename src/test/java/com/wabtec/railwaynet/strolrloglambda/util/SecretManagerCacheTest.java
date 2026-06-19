// Copyright Wabtec Inc. 2025. All rights reserved
package com.wabtec.railwaynet.strolrloglambda.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Tests for {@link SecretManagerCache}, which is injected as a {@link SecretResolver}.
 *
 * The load-bearing case is {@code resolve_failsClosed_whenUnreachable}: it locks in the
 * security fix that replaced a hardcoded fail-open "default-db-password" fallback with a
 * fail-closed throw. If a future refactor reintroduces any fallback, this test fails.
 */
class SecretManagerCacheTest {

    private static SecretsManagerClient stubReturning(String value) {
        SecretsManagerClient stub = mock(SecretsManagerClient.class);
        when(stub.getSecretValue(any(GetSecretValueRequest.class)))
            .thenReturn(GetSecretValueResponse.builder().secretString(value).build());
        return stub;
    }

    @Test
    void resolve_returnsValue_whenClientSucceeds() {
        SecretResolver resolver = new SecretManagerCache(stubReturning("resolved-secret-value"));

        assertEquals("resolved-secret-value", resolver.resolve("some-secret-name"));
    }

    @Test
    void resolve_failsClosed_whenUnreachable() {
        SecretsManagerClient stub = mock(SecretsManagerClient.class);
        when(stub.getSecretValue(any(GetSecretValueRequest.class)))
            .thenThrow(SdkClientException.create("simulated Secrets Manager outage"));
        SecretResolver resolver = new SecretManagerCache(stub);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> resolver.resolve("db-password-secret"));

        // Fail closed: surfaces the secret *name*, never a fallback credential value.
        assertTrue(ex.getMessage().contains("db-password-secret"),
            "exception should name the secret that could not be retrieved");
        assertFalse(ex.getMessage().contains("default-db-password"),
            "fail-open fallback credential must never reappear");
    }

    @Test
    void resolve_cachesWithinTtl_callsClientOnce() {
        SecretsManagerClient stub = stubReturning("cached-value");
        SecretManagerCache cache = new SecretManagerCache(stub);

        assertEquals("cached-value", cache.resolve("k"));
        assertEquals("cached-value", cache.resolve("k"));

        verify(stub, times(1)).getSecretValue(any(GetSecretValueRequest.class));
    }
}
