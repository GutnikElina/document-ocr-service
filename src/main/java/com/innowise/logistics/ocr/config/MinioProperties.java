/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "storage.minio")
public record MinioProperties(
    @NotNull URI endpoint,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String bucket,
    @NotNull Duration connectTimeout,
    @NotNull Duration writeTimeout,
    @NotNull Duration readTimeout
) { }
