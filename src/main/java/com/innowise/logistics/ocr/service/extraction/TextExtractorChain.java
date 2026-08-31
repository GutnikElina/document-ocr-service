/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import com.innowise.logistics.ocr.exception.TextExtractionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class TextExtractorChain implements TextExtractor {
    private final List<TextExtractor> extractors;

    @Override
    public ExtractedText extract(Path document) {
        TextExtractionException lastException = null;
        for (TextExtractor extractor : extractors) {
            try {
                ExtractedText result = extractor.extract(document);
                if (result.text() != null && !result.text().isBlank()) {
                    return result;
                }
                log.debug(
                    "Extractor {} returned empty text for {}",
                    extractor.getClass().getSimpleName(),
                    document
                );
            } catch (TextExtractionException e) {
                lastException = e;
                log.warn(
                    "Extractor {} failed for {}",
                    extractor.getClass().getSimpleName(),
                    document,
                    e
                );
            }
        }
        if (lastException != null) {
            throw new TextExtractionException(
                "All configured text extractors failed",
                lastException
            );
        }
        throw new TextExtractionException("All configured text extractors returned empty text");
    }
}
