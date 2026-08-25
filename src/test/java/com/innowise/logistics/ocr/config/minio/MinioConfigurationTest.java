/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.minio;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MinioConfigurationTest {
    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                ConfigurationPropertiesAutoConfiguration.class,
                ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(MinioConfiguration.class);

    @Test
    void shouldBindPropertiesAndCreateMinioClient() {
        // Act & Assert
        contextRunner
            .withPropertyValues(
                "storage.minio.endpoint=http://localhost:9000",
                "storage.minio.access-key=local-access-key",
                "storage.minio.secret-key=local-secret-key",
                "storage.minio.bucket=transport-documents",
                "storage.minio.connect-timeout=2s",
                "storage.minio.write-timeout=10s",
                "storage.minio.read-timeout=10s"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(MinioClient.class);
                assertThat(context).hasSingleBean(MinioProperties.class);
                MinioProperties properties = context.getBean(MinioProperties.class);
                assertThat(properties.endpoint())
                    .isEqualTo(URI.create("http://localhost:9000"));
                assertThat(properties.bucket())
                    .isEqualTo("transport-documents");
                assertThat(properties.connectTimeout())
                    .isEqualTo(Duration.ofSeconds(2));
                assertThat(properties.writeTimeout())
                    .isEqualTo(Duration.ofSeconds(10));
                assertThat(properties.readTimeout())
                    .isEqualTo(Duration.ofSeconds(10));
            });
    }

    @Test
    void shouldRejectConfigurationWithoutSecretKey() {
        // Act & Assert
        contextRunner
            .withPropertyValues(
                "storage.minio.endpoint=http://localhost:9000",
                "storage.minio.access-key=local-access-key",
                "storage.minio.bucket=transport-documents",
                "storage.minio.connect-timeout=2s",
                "storage.minio.write-timeout=10s",
                "storage.minio.read-timeout=10s"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class);
            });
    }
}
