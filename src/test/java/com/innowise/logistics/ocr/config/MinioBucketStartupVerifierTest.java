/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config;

import com.innowise.logistics.ocr.exception.ObjectStorageStartupException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioBucketStartupVerifierTest {
    private static final String REQUIRED_BUCKET = "transport-documents";

    private final MinioProperties properties = new MinioProperties(
        URI.create("http://localhost:9000"),
        "local-access-key",
        "local-secret-key",
        REQUIRED_BUCKET,
        Duration.ofSeconds(2),
        Duration.ofSeconds(10),
        Duration.ofSeconds(10)
    );

    private final MinioClient minioClient = mock(MinioClient.class);

    private final ApplicationArguments applicationArguments = mock(ApplicationArguments.class);

    private final MinioBucketStartupVerifier verifier =
        new MinioBucketStartupVerifier(minioClient, properties);

    @Test
    void shouldCompleteStartupWhenRequiredBucketExists() throws MinioException {
        // Arrange
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenReturn(true);

        // Act & Assert
        assertThatCode(() -> verifier.run(applicationArguments))
            .doesNotThrowAnyException();
        verify(minioClient).bucketExists(argThat(
            arguments -> REQUIRED_BUCKET.equals(arguments.bucket())
        ));
    }

    @Test
    void shouldAbortStartupWhenRequiredBucketIsMissing() throws MinioException {
        // Arrange
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> verifier.run(applicationArguments))
            .isInstanceOf(ObjectStorageStartupException.class)
            .hasMessageContaining(REQUIRED_BUCKET);
    }

    @Test
    void shouldAbortStartupWhenBucketAvailabilityCannotBeVerified() throws MinioException {
        // Arrange
        MinioException storageFailure = new MinioException("Connection refused");
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenThrow(storageFailure);

        // Act & Assert
        assertThatThrownBy(() -> verifier.run(applicationArguments))
            .isInstanceOf(ObjectStorageStartupException.class)
            .hasMessageContaining(REQUIRED_BUCKET);
    }
}
