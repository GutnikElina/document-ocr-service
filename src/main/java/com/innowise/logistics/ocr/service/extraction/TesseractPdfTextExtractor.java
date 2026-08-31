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

@Component
@RequiredArgsConstructor
public class TesseractPdfTextExtractor implements TextExtractor {
    private final ITesseract tesseract;

    @Override
    public ExtractedText extract(Path document) {
        try (PDDocument pdf = Loader.loadPDF(document.toFile())) {
            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            StringBuilder result = new StringBuilder();
            for (int page = 0; page < pdf.getNumberOfPages(); page++) {
                BufferedImage image =
                    pdfRenderer.renderImageWithDPI(page, 200);
                String pageText = tesseract.doOCR(image);
                if (pageText != null && !pageText.isBlank()) {
                    result
                        .append(pageText.trim())
                        .append(System.lineSeparator());
                }
                image.flush();
            }
            return new ExtractedText(
                result.toString().trim(),
                ExtractionMethod.TESSERACT
            );
        } catch (Exception e) {
            throw new TextExtractionException(
                "Tesseract failed to extract text from " + document,
                e
            );
        }
    }
}
