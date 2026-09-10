/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import com.innowise.logistics.ocr.exception.TextExtractionException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TesseractPdfTextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractAndJoinNonBlankPageText() throws Exception {
        // Arrange
        Path pdf = createPdfWithPages(3);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(any(BufferedImage.class)))
            .thenReturn(" first page ", " ", "second page\n");
        var extractor = new TesseractPdfTextExtractor(tesseract);

        // Act
        ExtractedText result = extractor.extract(pdf);

        // Assert
        assertThat(result.text()).isEqualTo(
            "first page" + System.lineSeparator() + "second page"
        );
        assertThat(result.method()).isEqualTo(ExtractionMethod.TESSERACT);
    }

    @Test
    void shouldTreatNullOcrResultAsEmptyText() throws Exception {
        // Arrange
        Path pdf = createPdfWithPages(1);
        ITesseract tesseract = mock(ITesseract.class);
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(null);
        var extractor = new TesseractPdfTextExtractor(tesseract);

        // Act
        ExtractedText result = extractor.extract(pdf);

        // Assert
        assertThat(result.text()).isEmpty();
    }

    @Test
    void shouldWrapPageOcrFailure() throws Exception {
        // Arrange
        Path pdf = createPdfWithPages(1);
        ITesseract tesseract = mock(ITesseract.class);
        TesseractException cause = new TesseractException("OCR failed");
        when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(cause);
        var extractor = new TesseractPdfTextExtractor(tesseract);

        // Act & Assert
        assertThatThrownBy(() -> extractor.extract(pdf))
            .isInstanceOf(TextExtractionException.class)
            .hasMessage("Tesseract failed to extract text from " + pdf)
            .hasRootCause(cause);
    }

    private Path createPdfWithPages(int pageCount) throws IOException {
        Path pdf = tempDir.resolve("document.pdf");
        try (var document = new PDDocument()) {
            for (int page = 0; page < pageCount; page++) {
                document.addPage(new PDPage(new PDRectangle(20, 20)));
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }
}
