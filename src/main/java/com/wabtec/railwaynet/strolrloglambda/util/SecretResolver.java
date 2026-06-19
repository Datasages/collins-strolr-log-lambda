// Copyright Wabtec Inc. 2025. All rights reserved
package com.wabtec.railwaynet.strolrloglambda.util;

/**
 * Resolves a named secret to its value. Abstracts the secret store so consumers
 * (e.g. {@code JdbcLogFileRepository}) depend on this interface rather than the
 * concrete AWS Secrets Manager implementation, and can be unit-tested with a stub.
 */
public interface SecretResolver {

    /**
     * @param secretId the name/id of the secret to retrieve
     * @return the resolved secret value
     * @throws IllegalStateException if the secret cannot be retrieved (fail closed)
     */
    String resolve(String secretId);
}
