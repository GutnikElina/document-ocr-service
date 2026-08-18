/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config;

import com.innowise.logistics.ocr.exception.ObjectStorageStartupException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class MinioBucketStartupVerifier implements ApplicationRunner {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public void run(@NonNull ApplicationArguments arguments) {
        String requiredBucket = properties.bucket();
        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(requiredBucket)
                    .build()
            );
            if (!bucketExists) {
                throw new ObjectStorageStartupException(
                    "Required MinIO bucket '%s' does not exist"
                        .formatted(requiredBucket)
                );
            }
        } catch (MinioException cause) {
            throw new ObjectStorageStartupException(
                "Unable to verify required MinIO bucket '%s'"
                    .formatted(requiredBucket),
                cause
            );
        }
    }
}
