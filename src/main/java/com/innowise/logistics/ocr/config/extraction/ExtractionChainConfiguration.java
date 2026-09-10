/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.config.extraction;

import com.innowise.logistics.ocr.service.extraction.TesseractPdfTextExtractor;
import com.innowise.logistics.ocr.service.extraction.TextExtractor;
import com.innowise.logistics.ocr.service.extraction.TextExtractorChain;
import com.innowise.logistics.ocr.service.extraction.TikaPdfTextExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ExtractionChainConfiguration {

    @Bean
    TextExtractor textExtractorChain(
        TikaPdfTextExtractor tika,
        TesseractPdfTextExtractor tesseract
    ) {
        return new TextExtractorChain(
            List.of(tika, tesseract)
        );
    }

}
