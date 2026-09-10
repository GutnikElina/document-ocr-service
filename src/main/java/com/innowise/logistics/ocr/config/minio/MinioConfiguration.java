/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.minio;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {

    @Bean(destroyMethod = "close")
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient minioClient = MinioClient.builder()
            .endpoint(properties.endpoint().toString())
            .credentials(
                properties.accessKey(),
                properties.secretKey()
            )
            .build();
        minioClient.setTimeout(
            properties.connectTimeout().toMillis(),
            properties.writeTimeout().toMillis(),
            properties.readTimeout().toMillis()
        );
        return minioClient;
    }

}
