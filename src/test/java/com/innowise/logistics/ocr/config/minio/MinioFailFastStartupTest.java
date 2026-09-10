/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.minio;

import com.innowise.logistics.ocr.exception.ObjectStorageStartupException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioFailFastStartupTest {
    private static final String REQUIRED_BUCKET = "transport-documents";

    @Test
    void shouldFailSpringApplicationStartupWhenBucketIsMissing() {
        // Arrange
        SpringApplication application =
            new SpringApplication(MissingBucketApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        application.setLogStartupInfo(false);

        // Act & Assert
        assertThatThrownBy(application::run)
            .isInstanceOf(ObjectStorageStartupException.class)
            .hasMessageContaining(REQUIRED_BUCKET);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MinioBucketStartupVerifier.class)
    static class MissingBucketApplication {
        @Bean
        MinioProperties minioProperties() {
            return new MinioProperties(
                URI.create("http://localhost:9000"),
                "local-access-key",
                "local-secret-key",
                "transport-documents",
                Duration.ofSeconds(2),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10)
            );
        }

        @Bean(destroyMethod = "")
        MinioClient minioClient() throws MinioException {
            MinioClient minioClient = mock(MinioClient.class);
            when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenReturn(false);
            return minioClient;
        }
    }
}
