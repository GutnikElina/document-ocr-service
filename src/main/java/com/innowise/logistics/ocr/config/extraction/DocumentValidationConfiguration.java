/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.extraction;

import com.innowise.logistics.ocr.config.file.DocumentProperties;
import com.innowise.logistics.ocr.service.validation.DocumentFileValidator;
import com.innowise.logistics.ocr.service.validation.PdfDocumentFileValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentValidationConfiguration {

    @Bean
    DocumentFileValidator documentFileValidator(DocumentProperties properties) {
        return new PdfDocumentFileValidator(
            properties.maxFileSize()
        );
    }

}
