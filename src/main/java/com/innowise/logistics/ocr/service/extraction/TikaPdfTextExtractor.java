/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import com.innowise.logistics.ocr.exception.TextExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Component
public class TikaPdfTextExtractor implements TextExtractor {
    private final Tika tika = new Tika();

    @Override
    public ExtractedText extract(Path document) {
        try {
            String text = tika.parseToString(document.toFile());
            return new ExtractedText(
                normalize(text),
                ExtractionMethod.TIKA
            );
        } catch (Exception e) {
            throw new TextExtractionException(
                "Tika failed to extract text from " + document,
                e
            );
        }
    }

    private String normalize(String text) {
        return Optional.ofNullable(text)
            .map(value -> value.replaceAll("\r\n?", "\n").trim())
            .orElse("");
    }
}
