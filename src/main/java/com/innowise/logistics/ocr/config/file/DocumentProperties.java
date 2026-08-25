/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "document.upload")
public record DocumentProperties(
    long maxFileSize,
    Duration stagingCleanupTimeout
) { }
