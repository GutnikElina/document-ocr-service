/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Aliaksandr Dudkin
 * SPDX-License-Identifier: Apache-2.0
 */

package com.innowise.logistics.ocr.service.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TikaPdfTextExtractorTest {

    @TempDir
    Path tempDir;

    private static final class TestPdfFactory {
        private TestPdfFactory() {
            throw new AssertionError();
        }

        static byte[] createPdf(String text) throws IOException {
            try (PDDocument document = new PDDocument();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                PDPage pdPage = new PDPage();
                document.addPage(pdPage);
                try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, pdPage)) {
                    contentStream.beginText();
                    contentStream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                    );
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText(text);
                    contentStream.endText();
                }
                document.save(output);
                return output.toByteArray();
            }
        }

        static byte[] createImageOnlyPdf() throws IOException {
            var image = new BufferedImage(
                300,
                100,
                BufferedImage.TYPE_INT_RGB
            );
            try (var document = new PDDocument();
                 var output = new ByteArrayOutputStream()) {
                var page = new PDPage();
                document.addPage(page);
                var pdfImage = LosslessFactory.createFromImage(
                    document,
                    image
                );
                try (var contentStream =
                         new PDPageContentStream(document, page)) {
                    contentStream.drawImage(
                        pdfImage,
                        50,
                        600,
                        300,
                        100
                    );
                }
                document.save(output);
                return output.toByteArray();
            }
        }
    }

    @Test
    void shouldExtractTextFromPdf() throws Exception {
        // Arrange
        Path pdf = tempDir.resolve("document.pdf");
        Files.write(
            pdf,
            TestPdfFactory.createPdf("ORDER-123")
        );
        TikaPdfTextExtractor extractor = new TikaPdfTextExtractor();

        // Act
        var result = extractor.extract(pdf);

        // Assert
        assertThat(result.text()).contains("ORDER-123");
    }

    @Test
    void shouldReturnEmptyTextForImageOnlyPdf() throws Exception {
        // Arrange
        Path pdf = tempDir.resolve("image-only.pdf");
        Files.write(
            pdf,
            TestPdfFactory.createImageOnlyPdf()
        );
        TikaPdfTextExtractor extractor = new TikaPdfTextExtractor();

        // Act
        var result = extractor.extract(pdf);

        // Assert
        assertThat(result.text()).isBlank();
    }
}
