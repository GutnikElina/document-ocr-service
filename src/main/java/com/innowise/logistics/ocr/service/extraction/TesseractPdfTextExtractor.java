/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import com.innowise.logistics.ocr.exception.TextExtractionException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class TesseractPdfTextExtractor implements TextExtractor {
    private static final int RENDER_DPI = 200;

    private final ITesseract tesseract;

    @Override
    public ExtractedText extract(Path document) {
        try (PDDocument pdf = Loader.loadPDF(document.toFile())) {
            var pdfRenderer = new PDFRenderer(pdf);
            var text = IntStream.range(0, pdf.getNumberOfPages())
                .mapToObj(page -> extractPageText(pdfRenderer, page))
                .filter(pageText -> !pageText.isBlank())
                .collect(Collectors.joining(System.lineSeparator()));

            return new ExtractedText(
                text,
                ExtractionMethod.TESSERACT
            );
        } catch (Exception e) {
            throw new TextExtractionException(
                "Tesseract failed to extract text from " + document,
                e
            );
        }
    }

    private String extractPageText(PDFRenderer renderer, int page) {
        BufferedImage image = null;
        try {
            image = renderer.renderImageWithDPI(page, RENDER_DPI);
            return Optional.ofNullable(tesseract.doOCR(image))
                .map(String::trim)
                .orElse("");
        } catch (Exception cause) {
            throw new TextExtractionException(
                "Failed to process OCR for page " + page,
                cause
            );
        } finally {
            if (Objects.nonNull(image)) {
                image.flush();
            }
        }
    }
}
